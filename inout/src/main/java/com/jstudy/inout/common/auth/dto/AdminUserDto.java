package com.jstudy.inout.common.auth.dto;

import com.jstudy.inout.common.auth.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;

public class AdminUserDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private Long storeId;     
        private UserStatus status;  
        private boolean isAdmin; 
    }

    @Getter
    @Builder
    public static class Summary {
        private long total;
        private long active;
        private long leave;
        private long locked;
    }

    @Getter
    @Builder
    public static class ListResponse {
        private Summary summary;
        private Page<UserListItem> users;
    }

    @Getter
    @Builder
    public static class UserListItem {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private Long storeId;
        private String storeName;
        private UserStatus status;
        private boolean isLocked;
        private boolean isAdmin;
        private LocalDateTime createdAt;
    }
}