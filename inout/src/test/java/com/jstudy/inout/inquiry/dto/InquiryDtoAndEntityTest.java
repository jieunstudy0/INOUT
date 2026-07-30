package com.jstudy.inout.inquiry.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.entity.InquiryComment;

class InquiryDtoAndEntityTest {

    @Test
    @DisplayName("Inquiry 도메인 메서드 검증 - markAsRead 호출 시 isRead가 true로 변경된다")
    void inquiry_MarkAsRead() {
        // given
        Inquiry inquiry = Inquiry.builder().isRead(false).build();

        // when
        inquiry.markAsRead();

        // then
        assertThat(inquiry.isRead()).isTrue();
    }

    @Test
    @DisplayName("InquiryComment 도메인 메서드 검증 - updateContent 호출 시 내용이 변경된다")
    void inquiryComment_UpdateContent() {
        // given
        InquiryComment comment = InquiryComment.builder().content("기존 내용").build();

        // when
        comment.updateContent("수정된 내용");

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("InquiryDetailResponse 변환 - 모든 댓글이 필터링 없이 리스트에 담기고, 대댓글은 parentId를 가진다")
    void inquiryDetailResponse_Mapping_FlatComments() {
        // given
        User author = User.builder().id(1L).name("김직원").build();
        Inquiry inquiry = Inquiry.builder()
                .id(100L)
                .title("문의합니다")
                .author(author)
                .originalFileName("증빙.png")
                .savedFilePath("/uploads/inquiries/uuid.png")
                .build();

        InquiryComment rootComment = InquiryComment.builder()
                .id(10L)
                .content("부모 댓글")
                .author(author)
                .parent(null) 
                .build();

        InquiryComment childComment = InquiryComment.builder()
                .id(11L)
                .content("자식 댓글")
                .author(author)
                .parent(rootComment) 
                .build();

        inquiry.getComments().addAll(List.of(rootComment, childComment));

        // when
        InquiryDetailResponse response = InquiryDetailResponse.from(inquiry);

        // then
        assertThat(response.getComments()).hasSize(2);
        assertThat(response.getOriginalFileName()).isEqualTo("증빙.png");
        assertThat(response.getSavedFilePath()).isEqualTo("/uploads/inquiries/uuid.png");

        CommentResponse firstComment = response.getComments().get(0);
        assertThat(firstComment.getId()).isEqualTo(10L); 
        assertThat(firstComment.getContent()).isEqualTo("부모 댓글");
        assertThat(firstComment.getParentId()).isNull();

        CommentResponse secondComment = response.getComments().get(1);
        assertThat(secondComment.getId()).isEqualTo(11L);
        assertThat(secondComment.getContent()).isEqualTo("자식 댓글");
        assertThat(secondComment.getParentId()).isEqualTo(10L); 
    }

    @Test
    @DisplayName("InquiryListResponse 변환 - 문의글의 총 댓글 개수를 정확히 계산한다")
    void inquiryListResponse_Mapping_CommentCount() {
        // given
        User author = User.builder().id(1L).name("김직원").build();
        Inquiry inquiry = Inquiry.builder().id(100L).title("제목").author(author).build();
        
        InquiryComment comment1 = InquiryComment.builder().author(author).build();
        InquiryComment comment2 = InquiryComment.builder().author(author).build();
        inquiry.getComments().addAll(List.of(comment1, comment2));

        // when
        InquiryListResponse response = InquiryListResponse.from(inquiry);

        // then
        assertThat(response.getCommentCount()).isEqualTo(2);
        assertThat(response.getAuthorName()).isEqualTo("김직원");
    }
}