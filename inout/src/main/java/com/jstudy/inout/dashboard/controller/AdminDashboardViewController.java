package com.jstudy.inout.dashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse;
import com.jstudy.inout.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminDashboardViewController {

    private final DashboardService dashboardService;

    @GetMapping("/admin/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {

        if (principal == null) {
            return "redirect:/user/login";
        }

        DashboardSummaryResponse response = dashboardService.getDashboardSummary(principal.getUser());

        model.addAttribute("summary", response);
        model.addAttribute("adminName", principal.getUser().getName());
        
        return "admin/dashboard";
    }
}
