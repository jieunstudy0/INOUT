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

@Tag(name = "직원 예치금 충전 신청 (미사용·예약)", description = "실무 정책상 충전 신청은 OWNER 전용입니다. EMP는 조회·결제만 허용합니다.")
@RestController
@RequestMapping("/api/emp/charges")
@RequiredArgsConstructor
public class ChargeEmpController {

    private final ChargeService chargeService;

    @Operation(summary = "예치금 충전 요청 (비활성)", description = "정책상 충전 신청은 OWNER만 가능합니다. 이 API는 호출 시 403을 반환합니다.")
    @PostMapping
    @PreAuthorize("denyAll()")
    public ResponseEntity<?> requestCharge(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ChargeDto.Request request) {
        
        chargeService.requestCharge(principal.getUser().getId(), request.getAmount());
        return ResponseResult.successWithMessage("예치금 충전 요청이 완료되었습니다.");
    }

    @Operation(summary = "내 충전 요청 내역 조회 (비활성)")
    @GetMapping
    @PreAuthorize("denyAll()")
    public ResponseEntity<?> getMyChargeRequests(@AuthenticationPrincipal CustomUserDetails principal) {
        
        return ResponseResult.success("충전 요청 내역 조회가 완료되었습니다.", 
                chargeService.getMyChargeRequests(principal.getUser().getId()));
    }
}