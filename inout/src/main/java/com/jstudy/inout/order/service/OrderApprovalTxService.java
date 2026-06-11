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

        List<OrderDetail> waitingDetails = order.getOrderDetails().stream()
                .filter(detail -> detail.getStatus().isWaiting())
                .toList();

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

            order.updateStatus(OrderStatus.COMPLETED);
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

        if (order.getStatus() == OrderStatus.PAID) {
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
}