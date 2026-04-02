package com.jstudy.inout.common.auth.dto;

import com.jstudy.inout.common.auth.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserUpdate {

    private String email;
    private String password;
    private String userName;
    
    @Size(max = 20, message = "연락처는 최대 20자까지 입력해야 합니다.")
    @NotBlank(message = "연락처는 필수 항목입니다.")
    private String phone;

    @NotNull(message = "매장을 선택해주세요.")
    private Long storeId; 
    
    public static UserUpdate of(User user) {
        return UserUpdate.builder()
                .email(user.getEmail())
                .password(user.getPassword())
                .userName(user.getName())
                .phone(user.getPhone())
                .storeId(user.getStore() != null ? user.getStore().getId() : null)
                .build();
    }
}
