package com.jstudy.inout.common.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserStatus;
import com.jstudy.inout.common.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceStatusTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("ACTIVE 직원은 로그인 UserDetails 반환")
    void load_active_ok() {
        User user = User.builder()
                .email("emp@test.com")
                .password("x")
                .status(UserStatus.ACTIVE)
                .deleted(false)
                .isLocked(false)
                .build();
        given(userRepository.findByEmail("emp@test.com")).willReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("emp@test.com");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("휴직(ON_LEAVE) 직원 로그인 차단")
    void load_onLeave_blocked() {
        User user = User.builder()
                .email("leave@test.com")
                .password("x")
                .status(UserStatus.ON_LEAVE)
                .deleted(false)
                .isLocked(false)
                .build();
        given(userRepository.findByEmail("leave@test.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("leave@test.com"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("휴직");
    }

    @Test
    @DisplayName("퇴사(RESIGNED) 직원 로그인 차단")
    void load_resigned_blocked() {
        User user = User.builder()
                .email("out@test.com")
                .password("x")
                .status(UserStatus.RESIGNED)
                .deleted(true)
                .isLocked(false)
                .build();
        given(userRepository.findByEmail("out@test.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("out@test.com"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("퇴사");
    }
}
