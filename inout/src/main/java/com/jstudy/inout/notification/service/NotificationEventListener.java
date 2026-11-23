package com.jstudy.inout.notification.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.notification.entity.Notification;
import com.jstudy.inout.notification.entity.NotificationType;
import com.jstudy.inout.notification.repository.NotificationRepository;
import com.jstudy.inout.notification.service.NotificationEvent.Target;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.event.OrderStateChangedEvent;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 업무 트랜잭션이 커밋된 뒤 알림을 저장한다.
 *
 * - AFTER_COMMIT: 롤백된 일에 대한 알림이 나가지 않는다.
 * - @Async: 커밋 직후에도 원래 요청 스레드는 DB 연결을 아직 쥐고 있다. 같은 스레드에서 새 트랜잭션을 열면
 *   연결을 하나 더 잡게 되어, 동시 요청이 많을 때 연결 풀이 바닥나 모두 멈춘다
 *   (StockConcurrencyTest 100건 동시 승인에서 실제로 재현됨). 그래서 메일 알림과 같이 별도 스레드에서 저장한다.
 * - REQUIRES_NEW: 커밋 이후에는 원래 트랜잭션에 쓸 수 없어서 새 트랜잭션을 연다 (별도 클래스라 프록시가 적용된다).
 * - fallbackExecution: 트랜잭션 밖에서 발행된 알림도 저장한다.
 * - 커밋 이후 경계라 예외를 잡아 로그만 남긴다 — 알림 실패가 이미 끝난 업무를 되돌리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final String ROLE_OWNER = "ROLE_OWNER";
    private static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final OrderRequestRepository orderRequestRepository;

    @Async("applicationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationEvent event) {
        try {
            save(event);
        } catch (Exception e) {
            log.error("[알림] 저장 실패: type={}", event.type(), e);
        }
    }

    /** 본사 발주 승인·반려(기존 이벤트)를 요청한 직원과 그 매장 점주에게 알린다. */
    @Async("applicationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStateChanged(OrderStateChangedEvent event) {
        try {
            OrderRequest order = orderRequestRepository.findById(event.orderId()).orElse(null);
            if (order == null) {
                return;
            }
            User requester = order.getRequestUser();
            Long storeId = requester.getStore() != null ? requester.getStore().getId() : null;
            NotificationType type;
            String title;
            if (order.getStatus() == OrderStatus.APPROVED || order.getStatus() == OrderStatus.COMPLETED) {
                type = NotificationType.ORDER_APPROVED;
                title = "발주 #" + order.getId() + "이(가) 본사 승인되었습니다";
            } else if (order.getStatus() == OrderStatus.REJECTED) {
                type = NotificationType.ORDER_REJECTED;
                title = "발주 #" + order.getId() + "이(가) 반려되었습니다";
            } else {
                return;
            }
            List<Target> targets = new ArrayList<>();
            targets.add(Target.user(requester.getId(), "/emp/orders"));
            if (storeId != null) {
                targets.add(Target.storeOwners(storeId, "/owner/orders"));
            }
            save(new NotificationEvent(type, title, "자세한 내용은 발주 내역에서 확인해 주세요.", null, targets));
        } catch (Exception e) {
            log.error("[알림] 발주 상태 알림 저장 실패: orderId={}", event.orderId(), e);
        }
    }

    private void save(NotificationEvent event) {
        Map<Long, String> linkByRecipient = new LinkedHashMap<>();
        for (Target t : event.targets()) {
            for (Long userId : resolve(t)) {
                linkByRecipient.putIfAbsent(userId, t.link());
            }
        }
        if (event.actorId() != null) {
            linkByRecipient.remove(event.actorId());
        }
        List<Notification> rows = linkByRecipient.entrySet().stream()
                .map(e -> Notification.of(e.getKey(), event.type(), event.title(), event.message(), e.getValue()))
                .toList();
        notificationRepository.saveAll(rows);
    }

    private List<Long> resolve(Target t) {
        return switch (t.kind()) {
            case USER -> t.id() == null ? List.of() : List.of(t.id());
            case STORE_OWNERS -> userRepository.findIdsByStoreAndRole(t.id(), ROLE_OWNER);
            case STORE_EMPLOYEES -> userRepository.findIdsByStoreAndRole(t.id(), ROLE_EMPLOYEE);
            case ADMINS -> userRepository.findIdsByRole(ROLE_ADMIN);
        };
    }
}
