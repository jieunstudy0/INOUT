package com.jstudy.inout.common.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserDtoMappingTest {

    @Test
    @DisplayName("User 엔티티를 UserResponse DTO로 변환한다")
    void toUserResponse() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("김지은")
                .phone("010-1234-5678")
                .birthday(LocalDate.of(1995, 1, 1))
                .build();

        // when
        UserResponse response = UserResponse.of(user);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getName()).isEqualTo("김지은");
        assertThat(response.getPhone()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("User 엔티티를 UserUpdate DTO로 변환한다 (매장이 있는 경우)")
    void toUserUpdate() {
        // given
        Store store = Store.builder().id(5L).name("본점").address("서울").build();
        User user = User.builder()
                .email("test@test.com")
                .password("encoded!")
                .name("김지은")
                .phone("010-1234-5678")
                .birthday(LocalDate.of(1995, 1, 1))
                .store(store)
                .build();

        // when
        UserUpdate updateDto = UserUpdate.of(user);

        // then
        assertThat(updateDto.getUserName()).isEqualTo("김지은");
        assertThat(updateDto.getStoreId()).isEqualTo(5L);
    }
}