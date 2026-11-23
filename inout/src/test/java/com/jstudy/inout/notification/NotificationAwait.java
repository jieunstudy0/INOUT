package com.jstudy.inout.notification;

import java.util.List;
import com.jstudy.inout.notification.entity.Notification;
import com.jstudy.inout.notification.repository.NotificationRepository;

/** 알림은 커밋 후 별도 스레드(@Async)에서 저장되므로, 테스트에서 잠깐 기다렸다가 읽는다. */
public final class NotificationAwait {

    private static final long TIMEOUT_MS = 5_000;
    private static final long INTERVAL_MS = 50;

    private NotificationAwait() {
    }

    /** 알림이 최소 atLeast개 쌓일 때까지 기다린 뒤 돌려준다. 시간이 지나면 그때까지 쌓인 것을 돌려준다. */
    public static List<Notification> inbox(NotificationRepository repository, Long recipientId, int atLeast) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        List<Notification> found = repository.findAllByRecipientId(recipientId);
        while (found.size() < atLeast && System.currentTimeMillis() < deadline) {
            sleep();
            found = repository.findAllByRecipientId(recipientId);
        }
        return found;
    }

    /** 다른 비동기 저장이 끝날 시간을 준 뒤, 알림이 없음을 확인할 때 쓴다. */
    public static List<Notification> inboxAfterQuietPeriod(NotificationRepository repository, Long recipientId) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return repository.findAllByRecipientId(recipientId);
    }

    private static void sleep() {
        try {
            Thread.sleep(INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
