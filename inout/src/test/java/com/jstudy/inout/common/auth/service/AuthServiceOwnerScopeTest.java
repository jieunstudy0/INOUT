package com.jstudy.inout.common.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.jstudy.inout.common.auth.dto.OwnerUserDto;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserStatus;
import com.jstudy.inout.common.auth.repository.RefreshTokenRepository;
import com.jstudy.inout.common.auth.repository.RoleRepository;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.repository.UserRoleRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.MailTemplateCacheService;
import com.jstudy.inout.common.mail.config.MailComponent;
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

@ExtendWith(MockitoExtension.class)
class AuthServiceOwnerScopeTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private MailComponent mailComponent;
    @Mock private MailTemplateCacheService mailTemplateCacheService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StoreRepository storeRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(authService, "fromEmail", "noreply@inout.com");
    }

    @Test
    @DisplayName("점주 직원 상태 변경 실패 - 소속 매장 없는 점주는 STORE_REQUIRED")
    void updateEmployeeByOwner_fail_storeRequired() {
        // given
        User owner = User.builder().id(1L).email("owner@test.com").name("점주").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(owner));

        OwnerUserDto.UpdateRequest request = OwnerUserDto.UpdateRequest.builder()
                .status(UserStatus.RESIGNED)
                .build();

        // when & then
        assertThatThrownBy(() -> authService.updateEmployeeByOwner(1L, 2L, request))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode")
                .isEqualTo("STORE_REQUIRED");
    }

    @Test
    @DisplayName("점주 직원 상태 변경 실패 - 타 매장 직원이면 CROSS_STORE_FORBIDDEN")
    void updateEmployeeByOwner_fail_crossStore() {
        // given
        Store ownerStore = Store.builder().id(10L).name("지점1").address("서울").build();
        Store otherStore = Store.builder().id(20L).name("지점2").address("서울").build();
        User owner = User.builder().id(1L).email("owner@test.com").name("점주").store(ownerStore).build();
        User employee = User.builder().id(2L).email("emp@test.com").name("직원").store(otherStore).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(userRepository.findById(2L)).willReturn(Optional.of(employee));

        OwnerUserDto.UpdateRequest request = OwnerUserDto.UpdateRequest.builder()
                .status(UserStatus.RESIGNED)
                .build();

        // when & then
        assertThatThrownBy(() -> authService.updateEmployeeByOwner(1L, 2L, request))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode")
                .isEqualTo("CROSS_STORE_FORBIDDEN");
    }

    @Test
    @DisplayName("점주 직원 잠금 해제 실패 - 타 매장 직원이면 CROSS_STORE_FORBIDDEN")
    void unlockEmployeeByOwner_fail_crossStore() {
        // given
        Store ownerStore = Store.builder().id(10L).name("지점1").address("서울").build();
        Store otherStore = Store.builder().id(20L).name("지점2").address("서울").build();
        User owner = User.builder().id(1L).email("owner@test.com").name("점주").store(ownerStore).build();
        User employee = User.builder().id(2L).email("emp@test.com").name("직원").store(otherStore).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(userRepository.findById(2L)).willReturn(Optional.of(employee));

        // when & then
        assertThatThrownBy(() -> authService.unlockEmployeeByOwner(1L, 2L))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode")
                .isEqualTo("CROSS_STORE_FORBIDDEN");
    }
}
