package com.jstudy.inout.common.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.JpaAuditConfig;

@SpringBootTest(classes = AuthJpaTestApplication.class)
@ActiveProfiles("jpa-slice")
@Transactional
@Import(JpaAuditConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@inout.com")
                .password("encodedPassword")
                .name("김지은")
                .phone("010-1234-5678")
                .birthday(LocalDate.of(1995, 1, 1))
                .deleted(false)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("이메일로 사용자를 조회할 수 있다")
    void findByEmail() {
        Optional<User> foundUser = userRepository.findByEmail("test@inout.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("김지은");
    }

    @Test
    @DisplayName("이메일 존재 여부를 확인할 수 있다 (중복 검사)")
    void existsByEmail() {
        boolean exists = userRepository.existsByEmail("test@inout.com");
        boolean notExists = userRepository.existsByEmail("none@inout.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("이메일, 이름, 전화번호로 사용자를 정확히 찾을 수 있다 (비밀번호 찾기용)")
    void findByEmailAndNameAndPhone() {
        Optional<User> foundUser = userRepository.findByEmailAndNameAndPhone(
                "test@inout.com", "김지은", "010-1234-5678"
        );

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("초기화 키(ResetKey)로 사용자를 찾을 수 있다")
    void findByPasswordResetKey() {
        testUser.setPasswordResetInfo("sample-uuid-key");
        userRepository.save(testUser); 

        Optional<User> foundUser = userRepository.findByPasswordResetKey("sample-uuid-key");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@inout.com");
    }

    @Test
    @DisplayName("@Query 테스트: 활성화(deleted=false) 상태인 유저만 조회한다")
    void findByEmailActive() {
        // given
        User deletedUser = User.builder()
                .email("deleted@inout.com")
                .password("pass")
                .name("탈퇴자")
                .phone("010-0000-0000")
                .birthday(LocalDate.now())
                .deleted(true) 
                .build();
        userRepository.save(deletedUser);

        // when & then
        Optional<User> activeFound = userRepository.findByEmailActive("test@inout.com");
        Optional<User> deletedFound = userRepository.findByEmailActive("deleted@inout.com");

        assertThat(activeFound).isPresent(); 
        assertThat(deletedFound).isEmpty();  
    }
}