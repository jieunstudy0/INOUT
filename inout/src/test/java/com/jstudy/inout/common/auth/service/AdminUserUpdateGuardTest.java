package com.jstudy.inout.common.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.auth.dto.AdminUserDto;
import com.jstudy.inout.common.auth.dto.UserDetailDto;
import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 본사 회원 수정 — 관리자 권한 체크박스 회귀 테스트.
 *
 * 예전에는 boolean 필드 이름이 isAdmin이라 JSON에서 "isAdmin"을 읽지 못해 항상 false가 됐고,
 * 관리자 계정을 저장하기만 해도 관리자 권한이 지워졌다. 본인·마지막 관리자를 막는 장치도 없었다.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserUpdateGuardTest {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .findAndRegisterModules();

    @InjectMocks private AuthServiceImpl authService;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private MailComponent mailComponent;
    @Mock private MailTemplateCacheService mailTemplateCacheService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StoreRepository storeRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private final Role adminRole = Role.builder().roleId(1L).roleName("ROLE_ADMIN").build();

    @Test
    @DisplayName("화면이 보낸 \"isAdmin\": true를 그대로 읽는다")
    void updateRequest_readsIsAdmin() throws Exception {
        AdminUserDto.UpdateRequest request = JSON.readValue(
                "{\"status\":\"ACTIVE\",\"storeId\":null,\"isAdmin\":true}", AdminUserDto.UpdateRequest.class);

        assertThat(request.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("목록·상세 응답은 화면이 읽는 이름(isAdmin, isLocked)으로 나간다")
    void responses_useIsPrefixedNames() throws Exception {
        String item = JSON.writeValueAsString(AdminUserDto.UserListItem.builder().isAdmin(true).isLocked(true).build());
        String detail = JSON.writeValueAsString(UserDetailDto.Detail.builder().isAdmin(true).isLocked(true).build());

        assertThat(item).contains("\"isAdmin\":true", "\"isLocked\":true");
        assertThat(detail).contains("\"isAdmin\":true", "\"isLocked\":true");
    }

    @Test
    @DisplayName("본인 계정의 관리자 권한은 뺄 수 없다")
    void selfDemotion_forbidden() {
        User me = admin(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));

        assertThatThrownBy(() -> authService.updateUserByAdmin(1L, 1L, request(UserStatus.ACTIVE, false)))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode").isEqualTo("SELF_ADMIN_CHANGE_FORBIDDEN");
        verify(userRoleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("본인 계정을 퇴사 처리할 수 없다")
    void selfResign_forbidden() {
        User me = admin(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(me));

        assertThatThrownBy(() -> authService.updateUserByAdmin(1L, 1L, request(UserStatus.RESIGNED, true)))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode").isEqualTo("SELF_ADMIN_CHANGE_FORBIDDEN");
        assertThat(me.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("로그인할 수 있는 마지막 관리자의 권한은 뺄 수 없다")
    void lastAdmin_forbidden() {
        User other = admin(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(userRoleRepository.countByRoleNameAndUserStatus("ROLE_ADMIN", UserStatus.ACTIVE)).willReturn(1L);

        assertThatThrownBy(() -> authService.updateUserByAdmin(1L, 2L, request(UserStatus.ACTIVE, false)))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode").isEqualTo("LAST_ADMIN_REQUIRED");
        verify(userRoleRepository, never()).delete(any());
    }

    @Test
    @DisplayName("관리자가 둘 이상이면 다른 관리자의 권한을 뺄 수 있다")
    void otherAdmin_demoted_whenAnotherRemains() {
        User other = admin(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(userRoleRepository.countByRoleNameAndUserStatus("ROLE_ADMIN", UserStatus.ACTIVE)).willReturn(2L);
        given(roleRepository.findByRoleName("ROLE_ADMIN")).willReturn(Optional.of(adminRole));

        authService.updateUserByAdmin(1L, 2L, request(UserStatus.ACTIVE, false));

        verify(userRoleRepository).delete(other.getUserRoles().get(0));
    }

    @Test
    @DisplayName("관리자 권한을 유지한 채 저장하면 권한을 지우지 않는다")
    void keepAdmin_doesNotDelete() {
        User other = admin(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(other));
        given(roleRepository.findByRoleName("ROLE_ADMIN")).willReturn(Optional.of(adminRole));

        authService.updateUserByAdmin(1L, 2L, request(UserStatus.ACTIVE, true));

        verify(userRoleRepository, never()).delete(any());
    }

    private User admin(Long id) {
        User user = User.builder().id(id).email("admin" + id + "@test.com").name("관리자" + id).build();
        user.getUserRoles().add(UserRole.builder().user(user).role(adminRole).build());
        return user;
    }

    private static AdminUserDto.UpdateRequest request(UserStatus status, boolean isAdmin) {
        return AdminUserDto.UpdateRequest.builder().status(status).isAdmin(isAdmin).build();
    }
}
