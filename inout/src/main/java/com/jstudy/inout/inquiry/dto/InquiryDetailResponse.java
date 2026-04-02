package com.jstudy.inout.inquiry.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> comments;

    public static InquiryDetailResponse from(Inquiry inquiry) {
        List<CommentResponse> commentResponses = inquiry.getComments().stream()
                .filter(c -> c.getParent() == null)
                .map(CommentResponse::from)
                .collect(Collectors.toList());

        return InquiryDetailResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .authorName(inquiry.getAuthor().getName())
                .authorId(inquiry.getAuthor().getId())
                .isRead(inquiry.isRead())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .comments(commentResponses)
                .build();
    }
}