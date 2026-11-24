package com.jstudy.inout.notification.dto;

import java.time.LocalDateTime;
import com.jstudy.inout.notification.entity.Notification;
import com.jstudy.inout.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {
    private Long notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private String linkUrl;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .linkUrl(n.getLinkUrl())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
