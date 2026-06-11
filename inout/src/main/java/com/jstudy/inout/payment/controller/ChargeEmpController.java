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

@Tag(name = "직원 예치금 충전", description = "예치금 충전 요청 및 내역 조회 (EMPLOYEE)")
@RestController
@RequestMapping("/api/emp/charges")
@RequiredArgsConstructor
public class ChargeEmpController {

    private final ChargeService chargeService;

    @Operation(summary = "예치금 충전 요청", description = "관리자에게 예치금 충전을 요청합니다.")
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> requestCharge(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ChargeDto.Request request) {
        
        chargeService.requestCharge(principal.getUser().getId(), request.getAmount());
        return ResponseResult.successWithMessage("예치금 충전 요청이 완료되었습니다.");
    }

    @Operation(summary = "내 충전 요청 내역 조회", description = "본인이 요청한 예치금 충전 내역을 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getMyChargeRequests(@AuthenticationPrincipal CustomUserDetails principal) {
        
        return ResponseResult.success("충전 요청 내역 조회가 완료되었습니다.", 
                chargeService.getMyChargeRequests(principal.getUser().getId()));
    }
}