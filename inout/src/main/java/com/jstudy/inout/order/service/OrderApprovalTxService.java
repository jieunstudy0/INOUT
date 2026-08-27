package com.jstudy.inout.order.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.event.OrderStateChangedEvent;
import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.service.DepositService;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StockUsageHistory;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import com.jstudy.inout.payment.dto.DepositDto;
import com.jstudy.inout.stock.exception.NotEnoughStockException;

@Service
@RequiredArgsConstructor
public class OrderApprovalTxService {
    private static final String AI_VENDOR_NAME = "(주)본사지정협력사";
    private static final String AI_VENDOR_PHONE = "02-0000-0000";
    private static final String AI_INBOUND_ADDRESS = "본사 중앙창고 (AI 자동발주 입고)";

    private final OrderRequestRepository orderRequestRepository;
    private final StockUsageHistoryRepository usageHistoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final DeliveryService deliveryService;
    private final DepositService depositService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processSingleOrderApproval(Long orderId, Long adminId) {
        OrderRequest order = orderRequestRepository.findByIdForUpdateWithDetails(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));
        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자 정보를 찾을 수 없습니다.", 404, "ADMIN_NOT_FOUND"));
        boolean aiSuggestedOrder = isAiSuggestedOrder(order);

        List<OrderDetail> waitingDetails = order.getOrderDetails().stream()
                .filter(detail -> detail.getStatus().isWaiting())
                .toList();

        if (aiSuggestedOrder) {
            ensureAiProcurementSnapshot(order);
            for (OrderDetail detail : waitingDetails) {
                detail.updateStatus(OrderDetailStatus.APPROVED);
            }
            order.updateStatus(OrderStatus.APPROVED);
            order.updateProcessDate(LocalDateTime.now());
            deliveryService.createDeliveryIfAbsentForCompletedOrder(order);
            deliveryService.markAiInboundWaiting(order.getId());
            publishOrderStateChanged(order);
            return true;
        }

        try {
            Map<Long, Item> lockedItems = new LinkedHashMap<>();
            for (OrderDetail detail : waitingDetails) {
                Long itemId = detail.getItem().getItemId();
                Item item = lockedItems.computeIfAbsent(itemId, id ->
                        itemRepository.findByIdWithLock(id)
                                .orElseThrow(() -> new InoutException(
                                        "상품 정보가 없습니다.", 404, "ITEM_NOT_FOUND")));
                
                entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE);

                if (item.getCurrentStock() < detail.getRequestQuantity()) {
                    throw NotEnoughStockException.withCurrentStock(
                            item.getCurrentStock(), detail.getRequestQuantity());
                }
            }

            for (OrderDetail detail : waitingDetails) {
                Item item = lockedItems.get(detail.getItem().getItemId());
                item.removeStock(detail.getRequestQuantity());

                StockUsageHistory usage = StockUsageHistory.builder()
                        .item(item)
                        .user(adminUser)
                        .usageQuantity(detail.getRequestQuantity())
                        .resultStock(item.getCurrentStock())
                        .memo("발주 승인 (주문번호: " + orderId + ")")
                        .build();
                usageHistoryRepository.save(usage);

                detail.updateStatus(OrderDetailStatus.APPROVED);
            }

            order.updateStatus(OrderStatus.APPROVED);
            order.updateProcessDate(LocalDateTime.now());
            deliveryService.createDeliveryIfAbsentForCompletedOrder(order);
            publishOrderStateChanged(order);
            return true;

        } catch (NotEnoughStockException e) {
            depositService.refundDeposit(
                    order.getRequestUser().getId(), 
                    adminId,                        
                    DepositDto.RefundRequest.builder()
                            .amount(order.getTotalPrice())
                            .description("재고 부족으로 인한 시스템 자동 취소 및 환불")
                            .build()
            );

            order.updateStatus(OrderStatus.REJECTED);
            order.updateRejectReason("재고 부족 자동 취소: " + e.getMessage());
            order.updateProcessDate(LocalDateTime.now());

            for (OrderDetail detail : waitingDetails) {
                detail.updateStatus(OrderDetailStatus.REJECTED);
            }

            publishOrderStateChanged(order);
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleOrderRejection(Long orderId, Long adminId, String reason) {
        OrderRequest order = orderRequestRepository.findByIdForUpdateWithDetails(orderId)
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));

        if (order.getStatus().isAwaitingHq()) {
            depositService.refundDeposit(
                    order.getRequestUser().getId(),
                    adminId,                   
                    DepositDto.RefundRequest.builder()
                            .amount(order.getTotalPrice())
                            .description("관리자 전체 반려로 인한 예치금 환불 (주문번호: #" + orderId + ")")
                            .build()
            );
        }

        order.updateStatus(OrderStatus.REJECTED);
        order.updateRejectReason(reason);
        order.updateProcessDate(LocalDateTime.now());

        for (OrderDetail detail : order.getOrderDetails()) {
            detail.updateStatus(OrderDetailStatus.REJECTED);
        }

        publishOrderStateChanged(order);
    }

    private void publishOrderStateChanged(OrderRequest order) {
        eventPublisher.publishEvent(new OrderStateChangedEvent(order.getId()));
    }

    private static boolean isAiSuggestedOrder(OrderRequest order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return false;
        }
        // 결제/재고 차감 우회는 모든 품목이 AI 제안인 경우에만 허용.
        return order.getOrderDetails().stream().allMatch(OrderDetail::isAiSuggested);
    }

    private static void ensureAiProcurementSnapshot(OrderRequest order) {
        String name = order.getReceiverName();
        String phone = order.getReceiverPhone();
        String address = order.getDestinationAddress();
        boolean missing = name == null || name.isBlank()
                || phone == null || phone.isBlank()
                || address == null || address.isBlank()
                || "미정".equals(name)
                || "미정".equals(phone)
                || (address != null && address.startsWith("미정"));
        if (missing) {
            order.updateReceiverSnapshot(AI_VENDOR_NAME, AI_VENDOR_PHONE, AI_INBOUND_ADDRESS);
        }
        String memo = order.getMemo();
        if (memo == null || !memo.contains("가상공급처")) {
            order.updateMemo((memo == null ? "" : memo + " ") + "[가상공급처:" + AI_VENDOR_NAME + "]");
        }
    }
}