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
    private LocalDateTime createdAt;

    public static InquiryListResponse from(Inquiry inquiry) {
        return InquiryListResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .authorName(inquiry.getAuthor().getName())
                .isRead(inquiry.isRead())
                .commentCount(inquiry.getComments().size())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}