package com.jstudy.inout.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.inquiry.dto.CommentCreateRequest;
import com.jstudy.inout.inquiry.service.InquiryCommentService;

@WebMvcTest(InquiryCommentController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class InquiryCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InquiryCommentService commentService;

    private UsernamePasswordAuthenticationToken mockPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).email("user@inout.com").name("직원").build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        mockPrincipal = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(mockPrincipal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("댓글 작성 API - 일반 댓글 (parentId 없음)")
    void createComment_Root() throws Exception {
        // given
        String requestJson = "{\"content\": \"문의 내용 확인했습니다.\"}";

        given(commentService.createComment(eq(100L), eq(1L), any(CommentCreateRequest.class)))
                .willReturn(10L); 

        // when & then
        mockMvc.perform(post("/api/inquiry/100/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("댓글이 등록되었습니다."))
                .andExpect(jsonPath("$.body").value(10));
    }

    @Test
    @DisplayName("댓글 작성 API - 답댓글 (parentId 있음)")
    void createComment_Reply() throws Exception {
        // given
        String requestJson = "{\"content\": \"추가 답변 드립니다.\", \"parentId\": 10}";

        given(commentService.createComment(eq(100L), eq(1L), any(CommentCreateRequest.class)))
                .willReturn(11L); 

        // when & then
        mockMvc.perform(post("/api/inquiry/100/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("답댓글이 등록되었습니다."))
                .andExpect(jsonPath("$.body").value(11));
    }

    @Test
    @DisplayName("댓글 수정 API - 성공")
    void updateComment() throws Exception {
        // given
        String requestJson = "{\"content\": \"내용을 수정합니다.\"}";

        // when & then
        mockMvc.perform(put("/api/inquiry/100/comments/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("댓글이 수정되었습니다."))
                .andExpect(jsonPath("$.body").value(10));
    }

    @Test
    @DisplayName("댓글 수정 API 실패 - 내용이 비어있을 경우 400 Bad Request (@Valid 작동)")
    void updateComment_Fail_BlankContent() throws Exception {
        // given
        String requestJson = "{\"content\": \"\"}";

        // when & then
        mockMvc.perform(put("/api/inquiry/100/comments/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest()); // @Valid에 의해 400 에러 발생 확인
    }

    @Test
    @DisplayName("댓글 삭제 API - 성공")
    void deleteComment() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/inquiry/100/comments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("댓글이 삭제되었습니다."))
                .andExpect(jsonPath("$.body").value(10));
    }
}