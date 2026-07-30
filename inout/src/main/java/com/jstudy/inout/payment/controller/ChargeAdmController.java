package com.jstudy.inout.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.payment.dto.ChargeDto;
import com.jstudy.inout.payment.service.ChargeService;

@Tag(name = "관리자 예치금 충전 관리", description = "가맹점 직원의 충전 요청 승인 및 반려 (ADMIN)")
@RestController
@RequestMapping("/api/admin/charges")
@RequiredArgsConstructor
public class ChargeAdmController {

    private final ChargeService chargeService;

    @Operation(summary = "승인 대기 중인 충전 요청 목록 조회", description = "상태가 PENDING인 모든 충전 요청을 조회합니다.")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingRequests() {
        return ResponseResult.success("대기 중인 충전 요청 목록을 불러왔습니다.", 
                chargeService.getPendingChargeRequests());
    }

    @Operation(summary = "충전 요청 승인", description = "가맹점의 충전 요청을 승인하고 실제 예치금을 지급합니다.")
    @PatchMapping("/{chargeId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveCharge(
            @PathVariable("chargeId") Long chargeId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        chargeService.approveCharge(principal.getUser().getId(), chargeId);
        return ResponseResult.successWithMessage("충전 요청이 승인되어 예치금이 지급되었습니다.");
    }

    @Operation(summary = "충전 요청 반려", description = "가맹점의 충전 요청을 반려합니다.")
    @PatchMapping("/{chargeId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectCharge(
            @PathVariable("chargeId") Long chargeId,
            @RequestBody ChargeDto.RejectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        chargeService.rejectCharge(principal.getUser().getId(), chargeId, request.getReason());
        return ResponseResult.successWithMessage("충전 요청이 반려되었습니다.");
    }
}