package com.jstudy.inout.common.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.jstudy.inout.common.auth.entity.RefreshToken;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.JpaAuditConfig;

@SpringBootTest(classes = AuthJpaTestApplication.class)
@ActiveProfiles("jpa-slice")
@Transactional
@Import(JpaAuditConfig.class)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private RefreshToken savedToken;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("token@test.com")
                .password("pass")
                .name("토큰유저")
                .phone("010-1111-1111")
                .birthday(LocalDate.now())
                .build();
        savedUser = userRepository.save(user);

        RefreshToken token = RefreshToken.builder()
                .user(savedUser)
                .token("sample-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        savedToken = refreshTokenRepository.save(token);
    }

    @Test
    @DisplayName("토큰 문자열로 RefreshToken 엔티티를 조회할 수 있다")
    void findByToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("sample-refresh-token");
        
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("token@test.com");
    }

    @Test
    @DisplayName("특정 유저 ID로 RefreshToken을 일괄 삭제할 수 있다 (로그아웃 기능)")
    void deleteByUser_Id() {
        // when
        refreshTokenRepository.deleteByUser_Id(savedUser.getId());
        
        // then
        Optional<RefreshToken> deleted = refreshTokenRepository.findByToken("sample-refresh-token");
        assertThat(deleted).isEmpty();
    }
}