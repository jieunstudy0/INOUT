package com.jstudy.inout.inquiry.dto;

import java.time.LocalDateTime;
import com.jstudy.inout.common.auth.util.UserDisplayNames;
import com.jstudy.inout.inquiry.entity.Inquiry;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryListResponse {

    private Long inquiryId;
    private String title;
    private String authorName;
    private boolean isRead;
    private int commentCount;
    private String aiCategory;
    private boolean hasAiDraft;
    private String targetType;
    private String writerRole;
    private LocalDateTime createdAt;

    public static InquiryListResponse from(Inquiry inquiry) {
        int commentCount = inquiry.getComments() != null ? inquiry.getComments().size() : 0;
        return InquiryListResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .authorName(UserDisplayNames.displayName(inquiry.getAuthor()))
                .isRead(inquiry.isRead())
                .commentCount(commentCount)
                .aiCategory(inquiry.getAiCategory())
                .hasAiDraft(inquiry.getAiDraftAnswer() != null && !inquiry.getAiDraftAnswer().isBlank())
                .targetType(inquiry.getTargetType() != null ? inquiry.getTargetType().name() : "ADMIN")
                .writerRole(resolveWriterRole(inquiry))
                .createdAt(inquiry.getCreatedAt())
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