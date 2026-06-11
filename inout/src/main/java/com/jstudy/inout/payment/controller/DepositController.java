package com.jstudy.inout.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.payment.dto.DepositDto;
import com.jstudy.inout.payment.dto.DepositEmpDto; 
import com.jstudy.inout.payment.service.DepositService;
import com.jstudy.inout.payment.service.DepositEmpService; 
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable; 
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "예치금 관리", description = "예치금 충전·환불 및 직원 조회 API")
@RestController
@RequestMapping("/api") 
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;
    private final DepositEmpService depositEmpService; 

    @Operation(summary = "나의 예치금 및 거래 내역 조회 (직원용)")
    @GetMapping("/emp/deposit") 
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'USER')") 
    public ResponseEntity<?> getMyDeposit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {

        DepositEmpDto.HistoryResponse response = 
                depositEmpService.getMyDepositHistory(userDetails.getUser().getId(), pageable);
        return ResponseResult.success("예치금 내역 조회가 완료되었습니다.", response);
    }

    @Operation(summary = "예치금 충전")
    @PostMapping("/deposit/charge") 
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'USER')") 
    public ResponseEntity<?> charge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody DepositDto.ChargeRequest request) {

        Long userId = userDetails.getUser().getId();
      
        DepositDto.Response responseData = depositService.chargeDeposit(userId, userId, request);
        return ResponseResult.successWithData(responseData);
    }

    @Operation(summary = "예치금 환불 (관리자용)")
    @PostMapping("/deposit/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> refund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody DepositDto.RefundRequest request) {

        Long adminId = userDetails.getUser().getId();
        DepositDto.Response responseData = depositService.refundDeposit(
                request.getTargetUserId(), adminId, request);

        return ResponseResult.successWithData(responseData);
    }
}