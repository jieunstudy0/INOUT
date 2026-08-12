package com.jstudy.inout.common.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnlockUserTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("unlockUser — isLocked=false, loginFailCount=0")
    void unlockUser_resetsLockAndFailCount() {
        User user = User.builder()
                .id(7L)
                .email("locked@test.com")
                .password("x")
                .name("잠김")
                .phone("010")
                .loginFailCount(5)
                .isLocked(true)
                .build();
        given(userRepository.findById(7L)).willReturn(Optional.of(user));

        authService.unlockUser(7L);

        assertThat(user.isLocked()).isFalse();
        assertThat(user.getLoginFailCount()).isZero();
    }
}
