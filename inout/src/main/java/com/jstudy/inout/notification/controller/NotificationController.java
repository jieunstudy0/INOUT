package com.jstudy.inout.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "알림", description = "내 알림 목록·안 읽은 개수·읽음 처리 (모든 역할)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 (최신순)")
    @GetMapping
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal CustomUserDetails principal,
                                                @RequestParam(name = "page", defaultValue = "0") int page,
                                                @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseResult.success("알림 조회가 완료되었습니다.",
                notificationService.getMyNotifications(principal.getUser().getId(), page, size));
    }

    @Operation(summary = "안 읽은 알림 개수")
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("안 읽은 알림 개수 조회가 완료되었습니다.",
                notificationService.getUnreadCount(principal.getUser().getId()));
    }

    @Operation(summary = "읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> markRead(@AuthenticationPrincipal CustomUserDetails principal,
                                      @PathVariable("notificationId") Long notificationId) {
        notificationService.markRead(principal.getUser().getId(), notificationId);
        return ResponseResult.successWithMessage("읽음 처리되었습니다.");
    }

    @Operation(summary = "모두 읽음 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("모두 읽음 처리되었습니다.",
                notificationService.markAllRead(principal.getUser().getId()));
    }
}
