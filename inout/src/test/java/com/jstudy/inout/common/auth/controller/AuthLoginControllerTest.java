package com.jstudy.inout.common.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.dto.UserLogin;
import com.jstudy.inout.common.auth.entity.RefreshToken;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.RefreshTokenRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.jwt.JwtTokenProvider;
import com.jstudy.inout.common.jwt.dto.JwtToken;

@WebMvcTest(AuthLoginController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false) 
class AuthLoginControllerTest {

    @Autowired
    private MockMvc mockMvc; 

    @Autowired
    private ObjectMapper objectMapper; 

    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private RefreshTokenRepository refreshTokenRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private AuthService authService;

    @Test
    @DisplayName("로그인 성공 - 올바른 정보 입력 시 토큰을 반환한다")
    void login_Success() throws Exception {
        // given
        UserLogin loginDto = new UserLogin("test@test.com", "password123!");
        User user = User.builder().id(1L).email("test@test.com").password("enc").build();
        CustomUserDetails principal = new CustomUserDetails(user);
        JwtToken mockToken = JwtToken.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        doNothing().when(authService).loginSuccess(anyString());
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.generateToken(any())).willReturn(mockToken);
        given(refreshTokenRepository.findByUser(any())).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk()) 
                .andExpect(jsonPath("$.header.message").value("로그인 성공"))
                .andExpect(jsonPath("$.body.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.body.role").value("ROLE_EMPLOYEE"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 틀림 시 401 에러를 반환한다")
    void login_Fail_WrongPassword() throws Exception {
        // given
        UserLogin loginDto = new UserLogin("test@test.com", "wrong!");

        doNothing().when(authService).loginFailed(anyString());
        given(authenticationManager.authenticate(any()))
                .willThrow(new RuntimeException("Bad credentials"));

        // when & then
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized()) 
                .andExpect(jsonPath("$.header.message")
                        .value("이메일 또는 비밀번호가 잘못되었거나, 5회 이상 실패하여 계정이 잠겼습니다."));
    }

    @Test
    @DisplayName("토큰 갱신 성공 - 유효한 리프레시 토큰 전달 시 새 액세스 토큰 반환")
    void refresh_Success() throws Exception {
        // given
        String reqJson = "{\"refreshToken\": \"valid-refresh-token\"}";
        User user = User.builder().email("test@test.com").build();
        RefreshToken rt = RefreshToken.builder()
                .token("valid-refresh-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1)) // 만료 안됨
                .build();

        given(refreshTokenRepository.findByToken("valid-refresh-token")).willReturn(Optional.of(rt));
        given(jwtTokenProvider.validateToken("valid-refresh-token")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(user)).willReturn("new-access-token");

        // when & then
        mockMvc.perform(post("/api/user/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("토큰이 갱신되었습니다."))
                .andExpect(jsonPath("$.body.accessToken").value("new-access-token"));
    }
}