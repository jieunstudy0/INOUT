package com.jstudy.inout.notification.service;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.notification.dto.NotificationResponse;
import com.jstudy.inout.notification.entity.Notification;
import com.jstudy.inout.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 내 알림 조회·읽음 처리. 항상 로그인한 사용자 본인의 알림만 다룬다. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    /** 이 기간이 지난 알림은 지운다 */
    static final long RETENTION_DAYS = 90;
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;

    public Page<NotificationResponse> getMyNotifications(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return notificationRepository.findByRecipientIdOrderByIdDesc(userId, PageRequest.of(Math.max(page, 0), safeSize))
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new InoutException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!n.getRecipientId().equals(userId)) {
            throw new InoutException(ErrorCode.NOT_OWNER, "본인의 알림만 읽음 처리할 수 있습니다.");
        }
        n.markRead();
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    /** 매일 새벽 4시, 보관 기간이 지난 알림을 지운다. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpired() {
        int deleted = notificationRepository.deleteOlderThan(LocalDateTime.now().minusDays(RETENTION_DAYS));
        if (deleted > 0) {
            log.info("[알림] 보관 기간({}일) 지난 알림 {}건 삭제", RETENTION_DAYS, deleted);
        }
    }
}
