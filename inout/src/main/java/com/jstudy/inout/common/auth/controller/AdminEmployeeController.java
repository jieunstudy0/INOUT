package com.jstudy.inout.common.auth.controller;

import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.dto.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 직원 계정", description = "직원 계정 잠금 해제 등 (ADMIN 전용)")
@RestController
@RequestMapping("/api/admin/employees")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final AuthService authService;

    @Operation(summary = "직원 계정 잠금 해제",
               description = "로그인 실패 횟수 초과로 잠긴 계정을 해제하고 실패 횟수를 0으로 초기화합니다.")
    @PatchMapping("/{id}/unlock")
    public ResponseEntity<?> unlockEmployee(@PathVariable("id") Long id) {
        authService.unlockUser(id);
        return ResponseResult.successWithMessage("해당 사용자의 계정 잠금이 성공적으로 해제되었습니다.");
    }
}
