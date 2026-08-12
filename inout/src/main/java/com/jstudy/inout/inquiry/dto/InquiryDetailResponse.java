package com.jstudy.inout.inquiry.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.jstudy.inout.common.auth.util.UserDisplayNames;
import com.jstudy.inout.inquiry.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryDetailResponse {

    private Long inquiryId;
    private String title;
    private String content;
    private String authorName;
    private Long authorId;
    private boolean isRead;
    private String originalFileName;
    private String savedFilePath;
    private String aiCategory;
    private String aiDraftAnswer;
    private String targetType;
    private String writerRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments;

    public static InquiryDetailResponse from(Inquiry inquiry) {
        List<CommentResponse> commentResponses = inquiry.getComments().stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());

        return InquiryDetailResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .authorName(UserDisplayNames.displayName(inquiry.getAuthor()))
                .authorId(inquiry.getAuthor() != null ? inquiry.getAuthor().getId() : null)
                .isRead(inquiry.isRead())
                .originalFileName(inquiry.getOriginalFileName())
                .savedFilePath(inquiry.getSavedFilePath())
                .aiCategory(inquiry.getAiCategory())
                .aiDraftAnswer(inquiry.getAiDraftAnswer())
                .targetType(inquiry.getTargetType() != null ? inquiry.getTargetType().name() : "ADMIN")
                .writerRole(resolveWriterRole(inquiry))
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .comments(commentResponses)
                .build();
    }

    private static String resolveWriterRole(Inquiry inquiry) {
        try {
            if (inquiry.getAuthor() != null && inquiry.getAuthor().getUserRoles() != null) {
                boolean isOwner = inquiry.getAuthor().getUserRoles().stream()
                        .anyMatch(ur -> ur.getRole() != null
                                && "ROLE_OWNER".equals(ur.getRole().getRoleName()));
                return isOwner ? "OWNER" : "EMPLOYEE";
            }
        } catch (Exception ignored) {}
        return "UNKNOWN";
    }
}