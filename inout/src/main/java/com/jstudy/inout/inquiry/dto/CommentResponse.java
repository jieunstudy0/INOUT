package com.jstudy.inout.inquiry.dto;

import java.time.LocalDateTime;
import com.jstudy.inout.inquiry.entity.InquiryComment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private Long id; 
    private String content;
    private String authorName;
    private String authorEmail; 
    private Long authorId; 
    private LocalDateTime createdAt;
    private Long parentId; 

    public static CommentResponse from(InquiryComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthor().getName())
                .authorEmail(comment.getAuthor().getEmail()) 
                .authorId(comment.getAuthor().getId())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .build();
    }
}