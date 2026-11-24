package com.jstudy.inout.stock.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.PendingReceipt;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.PendingReceiptLine;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.ReceiveLine;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.ReceiveRequest;
import com.jstudy.inout.stock.dto.store.StoreStockDtos.ReceiveResponse;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.entity.StoreStockChangeType;
import com.jstudy.inout.stock.entity.StoreStockHistory;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 배송 수령 확인 — 매장이 실제로 받은 수량만큼 매장 재고를 늘린다.
 *
 * 본사 재고는 이미 발주 승인 때 줄었으므로 여기서는 건드리지 않는다.
 * 덜 받은 수량은 발주 상세(receivedQuantity)와 이력 메모에 남겨, 나중에 수정 요청의 근거로 쓴다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StoreReceiptService {

    /** 배송 완료 후 이 기간 안에만 수령 확인을 받는다 (기능 도입 전 오래된 배송 제외) */
    static final long RECEIPT_WINDOW_DAYS = 14;

    private final DeliveryRepository deliveryRepository;
    private final StoreStockRepository storeStockRepository;
    private final StoreStockHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public List<PendingReceipt> getPendingReceipts(Long userId) {
        Long storeId = StoreStockService.storeIdOf(loadUser(userId));
        return deliveryRepository.findPendingReceiptsByStore(storeId, windowStart()).stream()
                .map(d -> {
                    OrderRequest order = d.getOrderRequest();
                    return PendingReceipt.builder()
                            .orderId(order.getId())
                            .deliveryStatus(d.getStatus().name())
                            .trackingNumber(d.getTrackingNumber())
                            .carrier(d.getCarrier())
                            .shippedAt(d.getShippedAt())
                            .deliveredAt(d.getDeliveredAt())
                            .requestUserName(order.getRequestUser().getName())
                            .lines(approvedDetails(order).stream()
                                    .map(od -> PendingReceiptLine.builder()
                                            .orderDetailId(od.getOrderDetailId())
                                            .itemId(od.getItem().getItemId())
                                            .itemName(od.getItem().getName())
                                            .unitDescription(od.getItem().getUnitDescription())
                                            .orderedQty(od.getRequestQuantity())
                                            .build())
                                    .toList())
                            .build();
                })
                .filter(r -> !r.getLines().isEmpty())
                .toList();
    }

    /**
     * 수령 확인. 모든 줄을 먼저 검사하고, 전부 통과했을 때만 매장 재고를 늘린다.
     * 배송 행을 잠가서 같은 배송을 두 번 수령 확인해도 한 번만 반영된다.
     */
    @Transactional
    public ReceiveResponse receive(Long userId, Long orderId, ReceiveRequest request) {
        User user = loadUser(userId);
        Long storeId = StoreStockService.storeIdOf(user);

        Delivery delivery = deliveryRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new InoutException(ErrorCode.DELIVERY_NOT_FOUND));
        OrderRequest order = delivery.getOrderRequest();
        User requester = order.getRequestUser();
        if (requester.getStore() == null || !storeId.equals(requester.getStore().getId())) {
            throw new InoutException(ErrorCode.CROSS_STORE_FORBIDDEN);
        }
        if (delivery.isReceived()) {
            throw new InoutException(ErrorCode.ALREADY_RECEIVED);
        }
        if (delivery.getStatus() == DeliveryStatus.READY) {
            throw new InoutException(ErrorCode.INVALID_DELIVERY_STATUS, "아직 출발하지 않은 배송은 수령 확인할 수 없습니다.");
        }
        if (delivery.getStatus() == DeliveryStatus.COMPLETED
                && delivery.getDeliveredAt() != null && delivery.getDeliveredAt().isBefore(windowStart())) {
            throw new InoutException(ErrorCode.INVALID_DELIVERY_STATUS,
                    "배송 완료 후 " + RECEIPT_WINDOW_DAYS + "일이 지나 수령 확인할 수 없습니다. 점주 재고 조정으로 맞춰 주세요.");
        }

        // 1) 검사: 승인된 품목마다 수령 수량이 하나씩 있어야 하고, 0 이상 발주 수량 이하여야 한다.
        Map<Long, OrderDetail> details = new HashMap<>();
        for (OrderDetail od : approvedDetails(order)) {
            details.put(od.getOrderDetailId(), od);
        }
        Map<Long, Integer> received = new HashMap<>();
        for (ReceiveLine line : request.getLines()) {
            OrderDetail od = details.get(line.getOrderDetailId());
            if (od == null || received.containsKey(line.getOrderDetailId())) {
                throw new InoutException(ErrorCode.INVALID_RECEIVED_QUANTITY, "이 배송에 없는 품목이 포함되어 있습니다.");
            }
            if (line.getReceivedQty() < 0 || line.getReceivedQty() > od.getRequestQuantity()) {
                throw new InoutException(ErrorCode.INVALID_RECEIVED_QUANTITY);
            }
            received.put(line.getOrderDetailId(), line.getReceivedQty());
        }
        if (received.size() != details.size()) {
            throw new InoutException(ErrorCode.INVALID_RECEIVED_QUANTITY, "모든 품목의 수령 수량을 입력해 주세요. (못 받은 품목은 0)");
        }

        // 2) 반영: 매장 재고 행을 itemId 오름차순으로 잠그고(없으면 만들고) 받은 만큼 늘린다.
        List<OrderDetail> ordered = details.values().stream()
                .sorted(Comparator.comparing(od -> od.getItem().getItemId()))
                .toList();
        int shortCount = 0;
        for (OrderDetail od : ordered) {
            int qty = received.get(od.getOrderDetailId());
            od.recordReceived(qty);
            if (qty < od.getRequestQuantity()) {
                shortCount++;
            }
            if (qty == 0) {
                continue;
            }
            StoreStock stock = storeStockRepository.findByStoreAndItemWithLock(storeId, od.getItem().getItemId())
                    .orElseGet(() -> storeStockRepository.save(StoreStock.create(user.getStore(), od.getItem())));
            stock.increase(qty);
            String memo = qty < od.getRequestQuantity()
                    ? String.format("[수령 발주#%d] 발주 %d → 수령 %d (%d개 부족)", order.getId(),
                            od.getRequestQuantity(), qty, od.getRequestQuantity() - qty)
                    : String.format("[수령 발주#%d] %d개", order.getId(), qty);
            historyRepository.save(StoreStockHistory.builder()
                    .storeStock(stock).user(user).type(StoreStockChangeType.RECEIVE)
                    .changeQty(qty).memo(memo).build());
        }

        delivery.confirmReceipt(user, LocalDateTime.now());
        log.info("[매장 재고] 수령 확인: orderId={}, storeId={}, 품목 {}개, 부족 {}개",
                orderId, storeId, ordered.size(), shortCount);
        return new ReceiveResponse(ordered.size(), shortCount);
    }

    private static List<OrderDetail> approvedDetails(OrderRequest order) {
        return order.getOrderDetails().stream()
                .filter(od -> od.getStatus() == OrderDetailStatus.APPROVED)
                .toList();
    }

    private static LocalDateTime windowStart() {
        return LocalDateTime.now().minusDays(RECEIPT_WINDOW_DAYS);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InoutException(ErrorCode.USER_NOT_FOUND));
    }
}
