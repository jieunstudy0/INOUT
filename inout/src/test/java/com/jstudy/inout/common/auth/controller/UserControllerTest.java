package com.jstudy.inout.common.auth.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.auth.dto.UserInput;
import com.jstudy.inout.common.auth.dto.UserInputFind;
import com.jstudy.inout.common.auth.dto.UserInputPassword;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.dto.ServiceResult;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // 단위 테스트이므로 Spring Security 필터 비활성화
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private UserRepository userRepository;
    @MockBean private PasswordEncoder passwordEncoder;

    private UsernamePasswordAuthenticationToken mockPrincipal(Long userId) {
        User user = User.builder().id(userId).email("test@test.com").name("테스터").build();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
    }

    @Test
    @DisplayName("회원가입 API - 올바른 정보 입력 시 가입에 성공한다")
    void register_Success() throws Exception {
        // given
        UserInput userInput = UserInput.builder()
                .email("test@test.com")
                .name("김지은")
                .password("pass123")
                .confirmPassword("pass123")
                .phone("010-1111-2222")
                .storeId(1L)
                .birthday(LocalDate.of(1995, 1, 1))
                .build();

        given(authService.addUser(any())).willReturn(ServiceResult.success());

        // when & then
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("회원가입 성공!"))
                .andExpect(jsonPath("$.body.redirectUrl").value("/user/login"));
    }

    @Test
    @DisplayName("이메일 중복확인 API - 사용 가능한 이메일이면 성공 메시지를 반환한다")
    void checkEmailDuplicate_Success() throws Exception {
        // given
        doNothing().when(authService).checkEmail("new@test.com"); 

        // when & then
        mockMvc.perform(get("/api/user/public/check-email")
                .param("email", "new@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("사용 가능한 이메일입니다."))
                .andExpect(jsonPath("$.body").value(nullValue()));
    }

    @Test
    @DisplayName("이메일 찾기 API - 이름과 전화번호가 일치하면 정보를 반환한다")
    void findUser_Success() throws Exception {
        // given
        UserInputFind inputFind = new UserInputFind(null, "김지은", "010-1111-2222");
        User user = User.builder().id(1L).email("found@test.com").name("김지은").phone("010-1111-2222").build();

        given(userRepository.findByNameAndPhone("김지은", "010-1111-2222")).willReturn(Optional.of(user));

        // when & then
        mockMvc.perform(post("/api/user/find")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputFind)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("사용자 정보를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.body.email").value("found@test.com"));
    }

    @Test
    @DisplayName("비밀번호 수정 API - 현재 비밀번호가 일치하면 변경에 성공한다")
    void updateUserPassword_Success() throws Exception {
        // given
        UserInputPassword passwordDto = new UserInputPassword("oldPass", "newPass123");
        User user = User.builder().id(1L).password("encodedOldPass").build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPass", "encodedOldPass")).willReturn(true); 
        given(passwordEncoder.encode("newPass123")).willReturn("encodedNewPass");
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(mockPrincipal(1L));
        SecurityContextHolder.setContext(context);

        // when & then
        mockMvc.perform(patch("/api/user/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.message").value("비밀번호가 성공적으로 변경되었습니다."))
                .andExpect(jsonPath("$.body").value(nullValue()));
        
        SecurityContextHolder.clearContext();
    }
}