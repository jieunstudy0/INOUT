package com.jstudy.inout.inquiry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.inquiry.dto.CommentCreateRequest;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.entity.InquiryComment;
import com.jstudy.inout.inquiry.repository.InquiryCommentRepository;
import com.jstudy.inout.inquiry.repository.InquiryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryCommentService {

    private final InquiryCommentRepository commentRepository;
    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 작성
     * - parentId가 null이면 일반 댓글
     * - parentId가 있으면 답댓글
     */
    @Transactional
    public Long createComment(Long inquiryId, Long userId, CommentCreateRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException("문의글을 찾을 수 없습니다.", 404, "INQUIRY_NOT_FOUND"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        InquiryComment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new InoutException("부모 댓글을 찾을 수 없습니다.", 404, "COMMENT_NOT_FOUND"));

            if (parent.getParent() != null) {
                throw new InoutException("답댓글에는 답댓글을 달 수 없습니다.", 400, "INVALID_COMMENT_DEPTH");
            }
        }

        InquiryComment comment = InquiryComment.builder()
                .inquiry(inquiry)
                .author(user)
                .content(request.getContent())
                .parent(parent)
                .build();

        return commentRepository.save(comment).getId();
    }

    /**
     * 댓글 삭제 (본인만 삭제 가능)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        InquiryComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new InoutException("댓글을 찾을 수 없습니다.", 404, "COMMENT_NOT_FOUND"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new InoutException("삭제 권한이 없습니다.", 403, "FORBIDDEN");
        }

        commentRepository.delete(comment);
    }

    /**
     * 댓글 수정 (본인만 수정 가능)
     */
    @Transactional
    public void updateComment(Long commentId, Long userId, String newContent) {
        InquiryComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new InoutException("댓글을 찾을 수 없습니다.", 404, "COMMENT_NOT_FOUND"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new InoutException("수정 권한이 없습니다.", 403, "FORBIDDEN");
        }

        comment.updateContent(newContent);
    }
}