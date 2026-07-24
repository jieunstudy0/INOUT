package com.jstudy.inout.common.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.common.dto.ServiceResult;
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

@WebMvcTest(OwnerUserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class OwnerUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Store store = Store.builder().id(10L).name("지점 1호").address("서울").build();
        User owner = User.builder().id(5L).email("owner1@test.com").name("점주1").store(store).build();
        CustomUserDetails details = new CustomUserDetails(owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, "", details.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("점주 직원 생성 성공")
    void createEmployee_success() throws Exception {
        // given
        given(authService.createEmployeeByOwner(eq(5L), any()))
                .willReturn(ServiceResult.success("직원 계정이 생성되었습니다."));

        String body = """
                {
                  "email": "newemp@test.com",
                  "name": "신규직원",
                  "password": "inout1234!",
                  "confirmPassword": "inout1234!",
                  "phone": "010-1111-2222",
                  "birthday": "1995-01-01"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/owner/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("직원 계정이 생성되었습니다."));
    }

    @Test
    @DisplayName("점주 직원 생성 실패 - DTO 유효성 검증 실패 시 400")
    void createEmployee_fail_validation() throws Exception {
        // when & then
        mockMvc.perform(post("/api/owner/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"name\":\"\",\"password\":\"1\"}"))
                .andExpect(status().isBadRequest());
    }
}
