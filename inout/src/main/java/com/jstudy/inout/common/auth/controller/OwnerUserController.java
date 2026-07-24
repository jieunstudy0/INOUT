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
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가맹점주 직원 관리", description = "소속 매장 직원 등록·조회·퇴사·비밀번호 초기화 (OWNER 전용)")
@RestController
@RequestMapping("/api/owner/users")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OwnerUserController {

    private final AuthService authService;

    @Operation(summary = "소속 매장 직원 목록 조회")
    @GetMapping
    public ResponseEntity<?> getUserList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            Pageable pageable) {

        OwnerUserDto.ListResponse response =
                authService.getOwnerUserList(requireOwnerId(principal), status, keyword, pageable);
        return ResponseResult.success("직원 목록 조회가 완료되었습니다.", response);
    }

    @Operation(summary = "소속 매장 직원 계정 생성")
    @PostMapping
    public ResponseEntity<?> createEmployee(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OwnerUserDto.CreateRequest request) {

        authService.createEmployeeByOwner(requireOwnerId(principal), request);
        return ResponseResult.successWithMessage("직원 계정이 생성되었습니다.");
    }

    @Operation(summary = "소속 매장 직원 상태 변경 (퇴사/휴직 등)")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long id,
            @RequestBody OwnerUserDto.UpdateRequest request) {

        authService.updateEmployeeByOwner(requireOwnerId(principal), id, request);
        return ResponseResult.successWithMessage("직원 정보가 변경되었습니다.");
    }

    @Operation(summary = "소속 매장 직원 계정 잠금 해제")
    @PatchMapping("/{id}/unlock")
    public ResponseEntity<?> unlockEmployee(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long id) {

        authService.unlockEmployeeByOwner(requireOwnerId(principal), id);
        return ResponseResult.successWithMessage("계정 잠금이 해제되었습니다.");
    }

    @Operation(summary = "소속 매장 직원 비밀번호 초기화 메일 발송")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("id") Long id) {

        authService.sendPasswordResetMailByOwner(requireOwnerId(principal), id);
        return ResponseResult.successWithMessage("비밀번호 초기화 링크가 발송되었습니다.");
    }

    private Long requireOwnerId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("인증 정보가 유효하지 않습니다.", 401, "UNAUTHORIZED");
        }
        return principal.getUser().getId();
    }
}
