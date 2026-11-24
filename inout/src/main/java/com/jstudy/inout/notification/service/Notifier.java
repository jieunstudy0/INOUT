package com.jstudy.inout.notification.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.jstudy.inout.notification.entity.NotificationType;
import com.jstudy.inout.notification.service.NotificationEvent.Target;
import lombok.RequiredArgsConstructor;

/**
 * 업무 코드가 알림을 보낼 때 쓰는 입구.
 * 여기서는 이벤트만 발행한다 — 실제 저장은 커밋 이후 NotificationEventListener가 한다.
 * 그래서 업무가 실패해 롤백되면 알림도 나가지 않고, 알림 저장이 실패해도 업무는 그대로 유지된다.
 */
@Component
@RequiredArgsConstructor
public class Notifier {

    private final ApplicationEventPublisher eventPublisher;

    public void send(NotificationType type, String title, String message, Long actorId, Target... targets) {
        eventPublisher.publishEvent(new NotificationEvent(type, title, message, actorId, List.of(targets)));
    }
}
