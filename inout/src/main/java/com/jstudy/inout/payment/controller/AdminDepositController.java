package com.jstudy.inout.payment.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 💡 추가
import org.springframework.web.bind.annotation.*;

import com.jstudy.inout.common.auth.dto.CustomUserDetails; // 💡 추가
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.payment.dto.AdminDepositDto;
import com.jstudy.inout.payment.dto.DepositDto;
import com.jstudy.inout.payment.service.AdminDepositService;
import com.jstudy.inout.payment.service.DepositService;

import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 예치금 관리", description = "가맹점 전체의 예치금 보유 현황 및 변동 내역 조회")
@RestController
@RequestMapping("/api/admin/deposits")
@RequiredArgsConstructor
public class AdminDepositController {

    private final AdminDepositService adminDepositService;
    private final DepositService depositService; 

    @Operation(summary = "전체 가맹점 예치금 내역 및 요약 조회")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllDepositHistories(
            @RequestParam(name = "storeId", required = false) Long storeId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "keyword", required = false) String keyword,
            Pageable pageable) {
            
        AdminDepositDto.ListResponse response = adminDepositService.getAdminDepositHistory(storeId, type, keyword, pageable);
        return ResponseResult.success("예치금 내역 조회가 완료되었습니다.", response);
    }

    @Operation(summary = "가맹점 목록 조회 (셀렉트박스용)")
    @GetMapping("/franchisees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getFranchiseeList() {
        List<AdminDepositDto.FranchiseeInfo> list = adminDepositService.getFranchiseeList();
        return ResponseResult.success("가맹점 목록 조회 성공", list);
    }

    @Operation(summary = "가맹점 예치금 수동 지급(충전)")
    @PostMapping("/charge")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> chargeDepositToUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AdminDepositDto.AdminChargeRequest request) {

        DepositDto.ChargeRequest chargeReq = DepositDto.ChargeRequest.builder()
                .amount(request.getAmount())
                .description(request.getDescription())
                .build();


        DepositDto.Response responseData = depositService.chargeDeposit(
                request.getTargetUserId(), 
                userDetails.getUser().getId(), 
                chargeReq);
        
        return ResponseResult.success("가맹점 수동 충전이 완료되었습니다.", responseData);
    }
}