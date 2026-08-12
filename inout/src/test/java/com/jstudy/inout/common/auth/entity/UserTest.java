package com.jstudy.inout.common.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("로그인 실패 횟수가 증가하며, 5회 도달 시 계정이 잠긴다")
    void increaseLoginFailCount() {
        // given
        User user = User.builder().loginFailCount(0).isLocked(false).build();

        // when - 4번 실패
        for (int i = 0; i < 4; i++) {
            user.increaseFailedAttempt();
        }

        // then
        assertThat(user.getLoginFailCount()).isEqualTo(4);
        assertThat(user.isLocked()).isFalse();

        // when - 5번째 실패
        user.increaseFailedAttempt();

        // then
        assertThat(user.getLoginFailCount()).isEqualTo(5);
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 횟수가 0으로 초기화된다")
    void resetLoginFailCount() {
        // given
        User user = User.builder().loginFailCount(3).build();

        // when
        user.resetLoginAttributes();

        // then
        assertThat(user.getLoginFailCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자의 연락처와 소속 매장 정보를 업데이트한다")
    void updateInfo() {
        // given
        User user = User.builder().phone("010-1111-1111").build();
        Store newStore = Store.builder().id(2L).name("판교점").build();

        // when
        user.updateInfo("010-2222-2222", newStore);

        // then
        assertThat(user.getPhone()).isEqualTo("010-2222-2222");
        assertThat(user.getStore().getName()).isEqualTo("판교점");
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 시 상태가 변경되고 키가 저장된다")
    void setPasswordResetInfo() {
        // given
        User user = User.builder().passwordResetYn(false).build();
        String resetKey = "sample-uuid-key";

        // when
        user.setPasswordResetInfo(resetKey);

        // then
        assertThat(user.isPasswordResetYn()).isTrue();
        assertThat(user.getPasswordResetKey()).isEqualTo(resetKey);
    }

    @Test
    @DisplayName("비밀번호 초기화 완료 시 관련 정보가 깔끔하게 지워진다")
    void clearPasswordResetInfo() {
        // given
        User user = User.builder()
                .passwordResetYn(true)
                .passwordResetKey("sample-uuid-key")
                .build();

        // when
        user.clearPasswordResetInfo();

        // then
        assertThat(user.isPasswordResetYn()).isFalse();
        assertThat(user.getPasswordResetKey()).isNull();
    }
}