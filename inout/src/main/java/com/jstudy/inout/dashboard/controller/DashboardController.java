package com.jstudy.inout.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse;
import com.jstudy.inout.dashboard.service.DashboardService;

import lombok.RequiredArgsConstructor;

@Tag(name = "대시보드", description = "실시간 현황 집계 (로그인 사용자 공통)")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "대시보드 현황 집계", description = "발주 상태별 건수, 저재고 품목 수 등 핵심 현황을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "집계 성공")
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@AuthenticationPrincipal CustomUserDetails principal) {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(principal.getUser());
        return ResponseResult.successWithData(response);
    }
}
