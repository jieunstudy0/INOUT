package com.jstudy.inout.common.auth.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.dto.OwnerUserDto;
import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가맹점주 직원 상태", description = "직원 계정 상태(재직/휴직/퇴사) 변경 — OWNER 전용")
@RestController
@RequestMapping("/api/owner/employees")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerEmployeeController {

    private final AuthService authService;

    @Operation(summary = "직원 계정 상태 변경",
               description = """
                       `status` 값:
                       - **ACTIVE**: 재직 (로그인 가능)
                       - **ON_LEAVE**: 휴직 (로그인 차단)
                       - **RESIGNED**: 퇴사 (로그인 차단, soft-delete)
                       """)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody OwnerUserDto.UpdateRequest request) {

        if (principal == null || principal.getUser() == null) {
            throw new InoutException("인증 정보가 유효하지 않습니다.", 401, "UNAUTHORIZED");
        }
        authService.updateEmployeeByOwner(principal.getUser().getId(), id, request);
        return ResponseResult.successWithMessage("직원 상태가 변경되었습니다.");
    }

    @Operation(summary = "직원 1일 예치금 사용 한도 설정",
               description = "dailyDepositLimit: 원 단위. null이면 무제한.")
    @PatchMapping("/{id}/deposit-limit")
    public ResponseEntity<?> updateDepositLimit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long id,
            @RequestBody OwnerUserDto.DepositLimitRequest request) {

        if (principal == null || principal.getUser() == null) {
            throw new InoutException("인증 정보가 유효하지 않습니다.", 401, "UNAUTHORIZED");
        }
        authService.updateEmployeeDepositLimitByOwner(principal.getUser().getId(), id, request);
        return ResponseResult.successWithMessage("1일 예치금 사용 한도가 변경되었습니다.");
    }
}
