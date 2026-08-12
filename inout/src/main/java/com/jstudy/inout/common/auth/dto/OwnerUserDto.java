package com.jstudy.inout.common.auth.dto;

import com.jstudy.inout.common.auth.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

public class OwnerUserDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @Email(message = "이메일 형식에 맞게 입력해 주세요.")
        @NotBlank(message = "이메일은 필수 항목입니다.")
        private String email;

        @NotBlank(message = "이름은 필수 항목입니다.")
        private String name;

        @Size(min = 4, message = "비밀번호는 4자 이상 입력해야 합니다.")
        @NotBlank(message = "비밀번호는 필수 항목입니다.")
        private String password;

        @NotBlank(message = "비밀번호 확인은 필수 항목입니다.")
        private String confirmPassword;

        @Size(max = 20, message = "연락처는 최대 20자까지 입력해야 합니다.")
        @NotBlank(message = "연락처는 필수 항목입니다.")
        private String phone;

        @NotNull(message = "생년월일은 필수 항목입니다.")
        private LocalDate birthday;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        @NotNull(message = "상태(status)는 필수입니다.")
        private UserStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepositLimitRequest {
        /** null이면 무제한 */
        private Long dailyDepositLimit;
    }

    @Getter
    @Builder
    public static class Summary {
        private long total;
        private long active;
        private long leave;
        private long resigned;
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
        private String roleName;
        private LocalDateTime createdAt;
        private Long dailyDepositLimit;
        private Long todayUsedDeposit;
    }
}
