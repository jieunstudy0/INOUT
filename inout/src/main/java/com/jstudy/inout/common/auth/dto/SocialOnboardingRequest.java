package com.jstudy.inout.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SocialOnboardingRequest(

        /** 사용자가 직접 확인·수정한 실명. 소셜 프로필 기본값이 채워지지만 자유롭게 변경 가능. */
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해 주세요.")
        String name,

        /**
         * "OWNER" 또는 "EMPLOYEE" 만 허용.
         * ADMIN은 소셜 온보딩으로 취득 불가.
         */
        @NotBlank(message = "역할을 선택해 주세요.")
        @Pattern(regexp = "^(OWNER|EMPLOYEE)$", message = "역할은 OWNER 또는 EMPLOYEE만 선택 가능합니다.")
        String role,

        @NotBlank(message = "연락처를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)")
        String phone,

        @NotNull(message = "생년월일을 입력해 주세요.")
        LocalDate birthday
) {}
