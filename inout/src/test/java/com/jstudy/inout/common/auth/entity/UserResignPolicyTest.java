package com.jstudy.inout.common.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.auth.util.UserDisplayNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserResignPolicyTest {

    @Test
    @DisplayName("퇴사 시 store=null, deleted=true Soft Delete")
    void resign_detachesStore_andSoftDeletes() {
        Store store = Store.builder().id(1L).name("강남점").address("서울").build();
        User user = User.builder()
                .name("김직원")
                .email("a@test.com")
                .password("x")
                .phone("010")
                .status(UserStatus.ACTIVE)
                .store(store)
                .deleted(false)
                .build();

        user.updateStatusAndStore(UserStatus.RESIGNED, store);

        assertThat(user.getStatus()).isEqualTo(UserStatus.RESIGNED);
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getStore()).isNull();
        assertThat(UserDisplayNames.displayName(user)).isEqualTo("김직원(퇴사자)");
        assertThat(UserDisplayNames.storeName(user)).isEqualTo("본점 (소속 없음)");
    }

    @Test
    @DisplayName("재직 복귀 시 deleted=false 및 매장 재배정")
    void rehire_restoresStore() {
        Store store = Store.builder().id(2L).name("홍대점").address("서울").build();
        User user = User.builder()
                .name("이직원")
                .email("b@test.com")
                .password("x")
                .phone("010")
                .status(UserStatus.RESIGNED)
                .deleted(true)
                .store(null)
                .build();

        user.updateStatusAndStore(UserStatus.ACTIVE, store);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getStore()).isEqualTo(store);
        assertThat(UserDisplayNames.displayName(user)).isEqualTo("이직원");
    }
}
