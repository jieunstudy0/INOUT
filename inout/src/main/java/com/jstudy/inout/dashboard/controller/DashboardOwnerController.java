package com.jstudy.inout.dashboard.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.dashboard.dto.DashboardOwnerResponse;
import com.jstudy.inout.dashboard.service.DashboardOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가맹점주 대시보드", description = "소속 매장 운영 KPI 집계 (OWNER 전용)")
@RestController
@RequestMapping("/api/owner/dashboard")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class DashboardOwnerController {

    private final DashboardOwnerService dashboardOwnerService;

    @Operation(summary = "매장 대시보드 요약",
               description = "오늘 발주 건수, 배송 중 건수, 연차 대기 건수 등 매장 운영 KPI를 반환합니다.")
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@AuthenticationPrincipal CustomUserDetails principal) {
        DashboardOwnerResponse response = dashboardOwnerService.getSummary(principal.getUser().getId());
        return ResponseResult.success("매장 대시보드 조회가 완료되었습니다.", response);
    }
}
