package com.jstudy.inout.common.auth.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 직원 상세 화면(본사·점주 공용) 응답과 기본 정보 수정 요청 */
public final class UserDetailDto {

    private UserDetailDto() {
    }

    @Getter
    @Builder
    public static class Detail {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private LocalDate birthday;
        private String profileImageUrl;
        private Long storeId;
        private String storeName;
        private UserStatus status;
        @JsonProperty("isLocked")
        private boolean isLocked;
        @JsonProperty("isAdmin")
        private boolean isAdmin;
        private List<String> roles;
        private Long dailyDepositLimit;
        private Long todayUsedDeposit;
        private String provider;
        private LocalDateTime createdAt;

        public static Detail from(User u) {
            List<String> roles = u.getUserRoles().stream().map(ur -> ur.getRole().getRoleName()).toList();
            return Detail.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .birthday(u.getBirthday())
                    .profileImageUrl(u.getProfileImageUrl())
                    .storeId(u.getStore() != null ? u.getStore().getId() : null)
                    .storeName(u.getStore() != null ? u.getStore().getName() : "본사 (소속 없음)")
                    .status(u.getStatus())
                    .isLocked(u.isLocked())
                    .isAdmin(roles.contains("ROLE_ADMIN"))
                    .roles(roles)
                    .dailyDepositLimit(u.getDailyDepositLimit())
                    .todayUsedDeposit(u.getTodayUsedDeposit())
                    .provider(u.getProvider())
                    .createdAt(u.getCreatedAt())
                    .build();
        }
    }

    /** 기본 정보(이름·연락처·사진) 수정. 사진은 먼저 업로드 API로 올리고 받은 경로를 보낸다. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfoRequest {
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.")
        private String name;

        @NotBlank(message = "연락처를 입력해 주세요.")
        @Size(max = 20, message = "연락처는 20자 이하로 입력해 주세요.")
        private String phone;

        @Size(max = 255, message = "사진 경로가 너무 깁니다.")
        private String profileImageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class StoreOption {
        private Long id;
        private String name;
    }
}
