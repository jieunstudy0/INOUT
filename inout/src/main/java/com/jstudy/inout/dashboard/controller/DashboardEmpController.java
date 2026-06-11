package com.jstudy.inout.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.dashboard.dto.DashboardEmpResponse;
import com.jstudy.inout.dashboard.service.DashboardEmpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/emp/dashboard")
@RequiredArgsConstructor
public class DashboardEmpController {

    private final DashboardEmpService dashboardEmpService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getSummary(@AuthenticationPrincipal CustomUserDetails principal) {
        DashboardEmpResponse response = dashboardEmpService.getSummary(principal.getUser().getId());
        return ResponseResult.success("대시보드 조회가 완료되었습니다.", response);
    }
}