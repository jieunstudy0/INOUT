package com.jstudy.inout.common.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.config.JpaAuditConfig;

@SpringBootTest(classes = AuthJpaTestApplication.class)
@ActiveProfiles("jpa-slice")
@Transactional
@Import(JpaAuditConfig.class)
class BasicReferenceRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("이름으로 매장(Store) 정보를 정확히 조회한다")
    void findStoreByName() {
        // given
        Store store = Store.builder().name("강남본점").address("서울시 강남구").build();
        storeRepository.save(store);

        // when
        Optional<Store> found = storeRepository.findByName("강남본점");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAddress()).isEqualTo("서울시 강남구");
    }

    @Test
    @DisplayName("Role 이름으로 권한(Role) 정보를 정확히 조회한다")
    void findRoleByName() {
        // given
        Role role = Role.builder().roleName("ROLE_ADMIN").build();
        roleRepository.save(role);

        // when
        Optional<Role> found = roleRepository.findByRoleName("ROLE_ADMIN");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getRoleName()).isEqualTo("ROLE_ADMIN");
    }
}