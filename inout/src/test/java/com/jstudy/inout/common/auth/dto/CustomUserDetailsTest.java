package com.jstudy.inout.common.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class CustomUserDetailsTest {

    @Test
    @DisplayName("권한 이름에 ROLE_ 접두사가 없으면 자동으로 붙여서 반환한다")
    void getAuthorities_AddRolePrefix() {
        // given
        Role role1 = Role.builder().roleName("ADMIN").build(); // ROLE_ 없음
        Role role2 = Role.builder().roleName("ROLE_USER").build(); // ROLE_ 있음
        
        User user = User.builder().build();
        user.getUserRoles().addAll(List.of(
                UserRole.builder().user(user).role(role1).build(),
                UserRole.builder().user(user).role(role2).build()
        ));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // when
        List<String> authorityNames = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // then
        assertThat(authorityNames).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }
}