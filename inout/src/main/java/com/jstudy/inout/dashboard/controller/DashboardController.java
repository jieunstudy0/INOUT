package com.jstudy.inout.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse;
import com.jstudy.inout.dashboard.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@AuthenticationPrincipal CustomUserDetails principal) {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(principal.getUser());
        return ResponseEntity.ok(response);
    }
}
