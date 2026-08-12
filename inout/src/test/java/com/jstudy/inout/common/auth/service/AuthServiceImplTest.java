package com.jstudy.inout.common.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.jstudy.inout.common.auth.dto.UserInput;
import com.jstudy.inout.common.auth.dto.UserPasswordResetInput;
import com.jstudy.inout.common.auth.dto.UserUpdate;
import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.RoleRepository;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.repository.UserRoleRepository;
import com.jstudy.inout.common.dto.ServiceResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.common.mail.dto.MailTemplate;
import com.jstudy.inout.common.mail.repository.MailTemplateRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private MailComponent mailComponent;
    @Mock private MailTemplateRepository mailTemplateRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StoreRepository storeRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "serverUrl", "http://localhost:8080");
    }

    @Test
    @DisplayName("회원가입 성공 - 정상적인 정보가 주어지면 DB에 저장되고 메일이 발송된다")
    void addUser_Success() {
        // given
        UserInput request = new UserInput();
        request.setEmail("test@test.com");
        request.setPassword("password123!");
        request.setName("테스터");
        request.setPhone("010-1234-5678");
        request.setStoreId(1L);
        request.setBirthday(LocalDate.of(1990, 1, 1));

        Store store = Store.builder().id(1L).name("강남점").build();
        Role role = Role.builder().roleName("ROLE_EMPLOYEE").build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
        given(storeRepository.findById(request.getStoreId())).willReturn(Optional.of(store));
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
        given(roleRepository.findByRoleName("ROLE_EMPLOYEE")).willReturn(Optional.of(role));
        given(mailComponent.send(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(true);

        // when
        ServiceResult result = authService.addUser(request);

        // then
        assertThat(result.isSuccess()).isTrue();
        verify(userRepository).save(any(User.class)); 
        verify(userRoleRepository).save(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일이면 예외가 발생한다")
    void addUser_Fail_DuplicateEmail() {
        // given
        UserInput request = new UserInput();
        request.setEmail("duplicate@test.com");

        User existingUser = User.builder().email("duplicate@test.com").build();
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> authService.addUser(request))
                .isInstanceOf(InoutException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("이메일 중복 체크 실패 - 이미 존재하는 이메일")
    void checkEmail_Fail_Exists() {
        // given
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.checkEmail("test@test.com"))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 성공 - 존재하는 회원이면 초기화 메일이 발송된다")
    void resetPassword_Success() {
        // given
        UserPasswordResetInput request = new UserPasswordResetInput();
        request.setEmail("test@test.com");
        request.setName("테스터");
        request.setPhone("010-1234-5678");

        User user = User.builder().email("test@test.com").name("테스터").build();
        MailTemplate template = MailTemplate.builder()
                .sendEmail("admin@test.com")
                .sendUserName("관리자")
                .title("비밀번호 초기화")
                .contents("링크: {SERVER_URL}/reset/{RESET_PASSWORD_KEY}")
                .build();

        given(userRepository.findByEmailAndNameAndPhone(request.getEmail(), request.getName(), request.getPhone()))
                .willReturn(Optional.of(user));
        given(mailTemplateRepository.findByTemplateId("USER_RESET_PASSWORD"))
                .willReturn(Optional.of(template));
        given(mailComponent.send(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(true);

        // when
        ServiceResult result = authService.resetPassword(request);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(user.isPasswordResetYn()).isTrue(); 
        assertThat(user.getPasswordResetKey()).isNotNull();
    }
    
    @Test
    @DisplayName("비밀번호 초기화 완료 성공 - 유효한 링크면 비밀번호가 변경된다")
    void completePasswordReset_Success() {
        // given
        User user = User.builder().email("test@test.com").password("oldPassword").build();
        user.setPasswordResetInfo("valid-uuid-key");

        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.now().minusMinutes(10));

        given(userRepository.findByPasswordResetKey("valid-uuid-key")).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPassword123!")).willReturn("encodedNewPassword");

        // when
        ServiceResult result = authService.completePasswordReset("valid-uuid-key", "newPassword123!");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(user.isPasswordResetYn()).isFalse(); // 상태 원복 확인
    }

    @Test
    @DisplayName("비밀번호 초기화 완료 실패 - 링크 유효시간(30분) 초과 시 실패 처리된다")
    void completePasswordReset_Fail_ExpiredLink() {
        // given
        User user = User.builder().email("test@test.com").build();
        user.setPasswordResetInfo("expired-uuid-key");

        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.now().minusMinutes(40));

        given(userRepository.findByPasswordResetKey("expired-uuid-key")).willReturn(Optional.of(user));

        // when
        ServiceResult result = authService.completePasswordReset("expired-uuid-key", "newPassword123!");

        // then
        assertThat(result.isFail()).isTrue();
        assertThat(result.getMessage()).contains("만료되었습니다");
        assertThat(user.isPasswordResetYn()).isFalse(); // 만료 시 상태 원복 확인
    }

    @Test
    @DisplayName("회원 정보 수정 성공 - 유효한 요청 시 전화번호와 매장이 변경된다")
    void updateUser_Success() {
        // given
        UserUpdate updateRequest = new UserUpdate();
        updateRequest.setPhone("010-9999-9999");
        updateRequest.setStoreId(2L);

        User user = User.builder().phone("010-1111-1111").build();
        Store newStore = Store.builder().id(2L).name("판교점").build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(storeRepository.findById(2L)).willReturn(Optional.of(newStore));

        // when
        ServiceResult result = authService.updateUser(1L, updateRequest);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getPhone()).isEqualTo("010-9999-9999");
        assertThat(user.getStore().getName()).isEqualTo("판교점");
    }
}