package com.jstudy.inout.inquiry.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.jstudy.inout.inquiry.entity.InquiryComment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private Long commentId;
    private String content;
    private String authorName;  
    private Long authorId; 
    private LocalDateTime createdAt;
    private List<CommentResponse> children; 

    public static CommentResponse from(InquiryComment comment) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .authorName(comment.getAuthor().getName())
                .authorId(comment.getAuthor().getId())
                .createdAt(comment.getCreatedAt())
                .children(comment.getChildren().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}