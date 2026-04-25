package com.jstudy.inout.inquiry.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.inquiry.dto.CommentCreateRequest;
import com.jstudy.inout.inquiry.dto.CommentUpdateRequest;
import com.jstudy.inout.inquiry.service.InquiryCommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/inquiry/{inquiryId}/comment")
@RequiredArgsConstructor
@Slf4j
public class InquiryCommentController {

    private final InquiryCommentService commentService;

    /**
     * 댓글 작성 (댓글 + 답댓글 통합)
     * - parentId 없으면 일반 댓글
     * - parentId 있으면 답댓글
     * POST /inquiry/{inquiryId}/comment
     */
        
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> createComment(
            @PathVariable Long inquiryId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUser().getId();
        log.info("댓글 작성 요청 - inquiryId: {}, userId: {}, parentId: {}", 
            inquiryId, userId, request.getParentId());

        Long commentId = commentService.createComment(inquiryId, userId, request);

        String message = request.getParentId() != null ? 
            "답댓글이 등록되었습니다." : "댓글이 등록되었습니다.";

        return ResponseResult.success(message, commentId);
    }

    /**
     * 댓글 수정 (본인만 가능)
     * PUT /inquiry/{inquiryId}/comment/{commentId}
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> updateComment(
            @PathVariable Long inquiryId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUser().getId();
        log.info("댓글 수정 요청 - commentId: {}, userId: {}", commentId, userId);

        commentService.updateComment(commentId, userId, request.getContent());

        return ResponseResult.success("댓글이 수정되었습니다.", commentId);
    }

    /**
     * 댓글 삭제 (본인만 가능)
     * DELETE /inquiry/{inquiryId}/comment/{commentId}
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long inquiryId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUser().getId();
        log.info("댓글 삭제 요청 - commentId: {}, userId: {}", commentId, userId);

        commentService.deleteComment(commentId, userId);

        return ResponseResult.success("댓글이 삭제되었습니다.", commentId);
    }
}