package com.jstudy.inout.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.inquiry.dto.CommentCreateRequest;
import com.jstudy.inout.inquiry.entity.Inquiry;
import com.jstudy.inout.inquiry.entity.InquiryComment;
import com.jstudy.inout.inquiry.repository.InquiryCommentRepository;
import com.jstudy.inout.inquiry.repository.InquiryRepository;

@ExtendWith(MockitoExtension.class)
class InquiryCommentServiceTest {

    @InjectMocks
    private InquiryCommentService inquiryCommentService;

    @Mock private InquiryCommentRepository commentRepository;
    @Mock private InquiryRepository inquiryRepository;
    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("댓글 작성 성공 - 일반 댓글 (parentId가 null)")
    void createComment_Success_Root() {
        // given
    	CommentCreateRequest request = CommentCreateRequest.builder()
    	        .content("첫 댓글입니다.")
    	        .parentId(null)
    	        .build();
        Inquiry inquiry = Inquiry.builder().id(1L).build();
        User user = User.builder().id(1L).build();
        InquiryComment savedComment = InquiryComment.builder().id(100L).build();

        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(commentRepository.save(any(InquiryComment.class))).willReturn(savedComment);

        // when
        Long commentId = inquiryCommentService.createComment(1L, 1L, request);

        // then
        assertThat(commentId).isEqualTo(100L);
        verify(commentRepository).save(any(InquiryComment.class));
    }

    @Test
    @DisplayName("답댓글 작성 실패 - 대댓글에 다시 대댓글을 달려고 하면 에러 발생")
    void createComment_Fail_InvalidDepth() {
        // given
    	CommentCreateRequest request = CommentCreateRequest.builder()
    	        .content("대댓글의 대댓글")
    	        .parentId(10L)
    	        .build(); 
        Inquiry inquiry = Inquiry.builder().id(1L).build();
        User user = User.builder().id(1L).build();
        
        InquiryComment rootComment = InquiryComment.builder().id(5L).build();
        InquiryComment parentComment = InquiryComment.builder().id(10L).parent(rootComment).build(); 

        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(commentRepository.findById(10L)).willReturn(Optional.of(parentComment));

        // when & then
        assertThatThrownBy(() -> inquiryCommentService.createComment(1L, 1L, request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("답댓글에는 답댓글을 달 수 없습니다.");
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 본인이 작성한 댓글이 아니면 권한 에러 발생")
    void deleteComment_Fail_Forbidden() {
        // given
        User author = User.builder().id(2L).build();
        InquiryComment comment = InquiryComment.builder().id(10L).author(author).build();

        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        // when & then 
        assertThatThrownBy(() -> inquiryCommentService.deleteComment(10L, 1L))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("삭제 권한이 없습니다.");
    }
}