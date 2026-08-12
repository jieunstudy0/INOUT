package com.jstudy.inout.order.event;

import com.jstudy.inout.delivery.service.DeliveryService;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeliveryEventListener {

    private final DeliveryService deliveryService;
    private final OrderRequestRepository orderRequestRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDelivery(OrderApprovedEvent event) {
        try {
            OrderRequest order = orderRequestRepository.findById(event.orderId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "배송 생성 대상 주문을 찾을 수 없습니다. orderId=" + event.orderId()));
            deliveryService.createDeliveryIfAbsentForCompletedOrder(order);
            log.info("배송 생성 완료: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("배송 생성 실패: orderId={}, message={}", event.orderId(), e.getMessage(), e);
        }
    }
}
