package com.jstudy.inout.common.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.auth.dto.AdminUserDto;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 사용자 관리", description = "계정 잠금 해제 등 관리자 전용 사용자 조작 (ADMIN 전용)")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @Operation(summary = "회원 목록 및 요약 조회", description = "필터 조건에 맞는 회원 목록과 상단 요약 통계를 반환합니다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserList(

            @RequestParam(name = "storeId", required = false) Long storeId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            Pageable pageable) {
        
        AdminUserDto.ListResponse response = authService.getAdminUserList(storeId, status, keyword, pageable);
        return ResponseResult.success("회원 목록 조회가 완료되었습니다.", response);
    }

    @Operation(summary = "회원 정보 및 권한 수정", description = "관리자가 직원의 매장, 재직 상태, 관리자 권한을 변경합니다.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserByAdmin(
            @PathVariable("id") Long id,
            @RequestBody AdminUserDto.UpdateRequest request) {
            
        authService.updateUserByAdmin(id, request);
        return ResponseResult.successWithMessage("회원 정보가 성공적으로 변경되었습니다.");
    }

    @Operation(summary = "계정 잠금 해제", description = "로그인 5회 실패로 잠긴 계정을 해제합니다.")
    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockUser(@PathVariable("id") Long id) {
        authService.unlockUser(id);
        return ResponseResult.successWithMessage("해당 사용자의 계정 잠금이 성공적으로 해제되었습니다.");
    }


    @Operation(summary = "비밀번호 초기화 메일 강제 발송", description = "관리자가 특정 직원의 비밀번호 초기화 메일을 강제로 전송합니다.")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendPasswordResetByAdmin(@PathVariable("id") Long id) {
        authService.sendPasswordResetMailByAdmin(id);
        return ResponseResult.successWithMessage("해당 사용자의 이메일로 비밀번호 초기화 링크가 발송되었습니다.");
    }
}