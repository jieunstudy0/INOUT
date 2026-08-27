package com.jstudy.inout.common.auth.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.dto.SocialOnboardingRequest;
import com.jstudy.inout.common.auth.service.SocialOnboardingService;
import com.jstudy.inout.common.dto.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "소셜 온보딩", description = "Google 소셜 최초 가입 후 역할 선택 및 프로필 완성 API")
@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialOnboardingController {

    private final SocialOnboardingService socialOnboardingService;

    @Operation(
            summary = "온보딩 완료",
            description = "ROLE_GUEST 사용자가 역할(OWNER/EMPLOYEE)과 필수 프로필(전화·생년월일)을 제출하면 " +
                          "ROLE_GUEST를 제거하고 선택 역할로 교체한 뒤 정식 JWT를 발급합니다."
    )
    @PostMapping("/complete-profile")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<?> completeProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid SocialOnboardingRequest request
    ) {
        var result = socialOnboardingService.complete(principal, request);
        return ResponseResult.success("온보딩이 완료되었습니다. 환영합니다!", result);
    }
}
