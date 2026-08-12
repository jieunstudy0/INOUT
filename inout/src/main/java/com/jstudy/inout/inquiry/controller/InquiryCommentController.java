package com.jstudy.inout.inquiry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "1:1 문의 답변(댓글) 관리", description = "문의글에 대한 본사 관리자의 답변 댓글 및 가맹점 직원의 추가 피드백 대댓글 처리 API")
@RestController
@RequestMapping("/api/inquiry/{inquiryId}/comments")
@RequiredArgsConstructor
@Slf4j
public class InquiryCommentController {

    private final InquiryCommentService commentService;

    @Operation(summary = "답변 및 댓글 등록 (일반 댓글 + 답댓글 통합)",
               description = """
                       문의사항 글에 답변이나 추가 의견 댓글을 등록합니다.
                       - **일반 댓글:** Request 바디의 `parentId`가 없으면(null) 1차 댓글로 등록됩니다.
                       - **답댓글(대댓글):** `parentId`가 있으면 해당 댓글 밑에 달리는 대댓글로 자동 처리됩니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글/답댓글 등록 성공 — 생성된 댓글 ID 반환"),
            @ApiResponse(responseCode = "404", description = "해당 문의글이나 부모 댓글을 찾을 수 없음")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> createComment(
            @Parameter(description = "댓글을 등록할 문의글 ID") @PathVariable("inquiryId") Long inquiryId,
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

    @Operation(summary = "댓글/답변 수정",
               description = "이미 등록된 댓글 본문내용을 수정합니다. 본인이 작성한 댓글만 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "본인이 작성하지 않은 댓글 수정 시도"),
            @ApiResponse(responseCode = "404", description = "댓글 찾을 수 없음")
    })
    @PutMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> updateComment(
            @Parameter(description = "문의글 ID") @PathVariable("inquiryId") Long inquiryId,
            @Parameter(description = "수정할 댓글 ID") @PathVariable("commentId") Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUser().getId();
        log.info("댓글 수정 요청 - commentId: {}, userId: {}", commentId, userId);

        commentService.updateComment(commentId, userId, request.getContent());

        return ResponseResult.success("댓글이 수정되었습니다.", commentId);
    }

    @Operation(summary = "댓글/답변 삭제",
               description = "작성된 댓글을 삭제 처리합니다. 본인이 작성한 댓글만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "본인이 작성하지 않은 댓글 삭제 시도"),
            @ApiResponse(responseCode = "404", description = "댓글 찾을 수 없음")
    })
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> deleteComment(
            @Parameter(description = "삭제할 댓글 ID") @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUser().getId();
        commentService.deleteComment(commentId, userId);
        return ResponseResult.success("댓글이 삭제되었습니다.", commentId);
    }
}