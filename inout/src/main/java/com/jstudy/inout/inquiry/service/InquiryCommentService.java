package com.jstudy.inout.inquiry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
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

    @Transactional
    public Long createComment(Long inquiryId, Long userId, CommentCreateRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InoutException(ErrorCode.INQUIRY_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException(ErrorCode.USER_NOT_FOUND));

        InquiryComment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new InoutException(ErrorCode.COMMENT_NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));

            if (parent.getParent() != null) {
                throw new InoutException(ErrorCode.INVALID_COMMENT_DEPTH);
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

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        InquiryComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new InoutException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new InoutException(ErrorCode.NOT_OWNER, "본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public void updateComment(Long commentId, Long userId, String newContent) {
        InquiryComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new InoutException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new InoutException(ErrorCode.NOT_OWNER, "본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        comment.updateContent(newContent);
    }
}