package com.jstudy.inout.common.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDailyDepositLimitTest {

    @Test
    @DisplayName("한도 내 사용 — todayUsedDeposit 누적")
    void consumeWithinLimit() {
        User user = User.builder()
                .dailyDepositLimit(100_000L)
                .todayUsedDeposit(20_000L)
                .build();

        user.consumeDailyDeposit(30_000L);

        assertThat(user.getTodayUsedDeposit()).isEqualTo(50_000L);
        assertThat(user.remainingDailyDepositLimit()).isEqualTo(50_000L);
    }

    @Test
    @DisplayName("한도 초과 시 IllegalStateException")
    void consumeExceedsLimit() {
        User user = User.builder()
                .dailyDepositLimit(50_000L)
                .todayUsedDeposit(40_000L)
                .build();

        assertThatThrownBy(() -> user.consumeDailyDeposit(20_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1일 예치금 사용 한도를 초과했습니다");
    }

    @Test
    @DisplayName("한도 null(무제한)이면 검증 통과")
    void unlimitedAllowsAnyAmount() {
        User user = User.builder().dailyDepositLimit(null).todayUsedDeposit(0L).build();
        user.consumeDailyDeposit(9_999_999L);
        assertThat(user.getTodayUsedDeposit()).isEqualTo(9_999_999L);
    }
}
