package com.jstudy.inout.order.event;

import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventListener {

    private final OrderRequestRepository orderRequestRepository;
    private final MailComponent mailComponent;

    @Async("applicationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOrderStateEmail(OrderStateChangedEvent event) {
        try {
            OrderRequest order = orderRequestRepository.findWithDetailsGraphById(event.orderId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "메일 발송 대상 주문을 찾을 수 없습니다. orderId=" + event.orderId()));
            mailComponent.sendOrderStateEmail(order);
        } catch (Exception e) {
            log.error("주문 상태 변경 메일 발송 실패: orderId={}, message={}", event.orderId(), e.getMessage(), e);
        }
    }
}
