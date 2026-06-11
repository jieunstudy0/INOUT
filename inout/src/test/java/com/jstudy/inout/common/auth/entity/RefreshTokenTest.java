package com.jstudy.inout.common.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    @DisplayName("토큰 값과 만료 시간이 정상적으로 업데이트된다")
    void updateToken() {
        // given
        RefreshToken refreshToken = RefreshToken.builder()
                .token("old-token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);

        // when
        refreshToken.updateToken("new-token", newExpiry);

        // then
        assertThat(refreshToken.getToken()).isEqualTo("new-token");
        assertThat(refreshToken.getExpiresAt()).isEqualTo(newExpiry);
    }

    @Test
    @DisplayName("만료 시간이 현재 시간보다 과거이면 isExpired는 true를 반환한다")
    void isExpired() {
        // given
        RefreshToken activeToken = RefreshToken.builder()
                .token("active-token")
                .expiresAt(LocalDateTime.now().plusMinutes(30)) 
                .build();
        
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusMinutes(1)) 
                .build();

        // when & then
        assertThat(activeToken.isExpired()).isFalse();
        assertThat(expiredToken.isExpired()).isTrue();
    }
}