package com.jstudy.inout.notification.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 화면 안 알림 한 건 (받는 사람 1명 기준).
 * 받는 사람은 ID만 들고 있다 — 알림은 많이 쌓이고 오래되면 지우므로 사용자와 묶지 않는다.
 * 제목·내용에는 금액·사유 같은 민감한 값을 넣지 않는다 (나중에 휴대폰 잠금 화면 푸시로도 보낼 예정).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notification",
        indexes = @Index(name = "idx_notification_recipient", columnList = "recipient_id, is_read, notification_id"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String message;

    /** 눌렀을 때 이동할 화면 경로 (예: /owner/stock-counts) */
    @Column(name = "link_url", length = 255)
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    private LocalDateTime readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Notification of(Long recipientId, NotificationType type, String title, String message, String linkUrl) {
        Notification n = new Notification();
        n.recipientId = recipientId;
        n.type = type;
        n.title = truncate(title, 100);
        n.message = truncate(message, 255);
        n.linkUrl = linkUrl;
        return n;
    }

    public void markRead() {
        if (!read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
