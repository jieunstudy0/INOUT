package com.jstudy.inout.inquiry.dto;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;

    public static InquiryListResponse from(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .authorName(inquiry.getAuthor().getName())
                .isRead(inquiry.isRead())
                .commentCount(inquiry.getComments().size())
                .aiCategory(inquiry.getAiCategory())
                .hasAiDraft(inquiry.getAiDraftAnswer() != null && !inquiry.getAiDraftAnswer().isBlank())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}