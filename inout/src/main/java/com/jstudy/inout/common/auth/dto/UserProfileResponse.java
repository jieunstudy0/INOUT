package com.jstudy.inout.common.auth.dto;

import com.jstudy.inout.common.auth.entity.User;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class UserProfileResponse {
    private String email; 
    private String name;
    private String storeName;
    private LocalDate birthday;
    private String phone;
    private String status;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .storeName(user.getStore() != null ? user.getStore().getName() : "")
                .birthday(user.getBirthday())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .build();
    }
}