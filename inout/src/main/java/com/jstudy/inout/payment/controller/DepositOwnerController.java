package com.jstudy.inout.payment.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.payment.dto.ChargeDto;
import com.jstudy.inout.payment.dto.DepositEmpDto;
import com.jstudy.inout.payment.service.ChargeService;
import com.jstudy.inout.payment.service.DepositEmpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가맹점주 예치금", description = "매장 예치금 잔액·이력 조회 및 충전 신청 (OWNER 전용)")
@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class DepositOwnerController {

    private final DepositEmpService depositEmpService;
    private final ChargeService chargeService;

    @Operation(summary = "매장 예치금 잔액 및 거래 내역 조회")
    @GetMapping("/deposit")
    public ResponseEntity<?> getStoreDeposit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long storeId = requireStoreId(principal);
        DepositEmpDto.HistoryResponse response =
                depositEmpService.getStoreDepositHistory(storeId, pageable);
        return ResponseResult.success("매장 예치금 내역 조회가 완료되었습니다.", response);
    }

    @Operation(summary = "예치금 충전 신청",
               description = "본사 관리자 승인이 필요한 매장 예치금 충전 요청을 생성합니다.")
    @PostMapping("/charges")
    public ResponseEntity<?> requestCharge(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ChargeDto.Request request) {

        chargeService.requestCharge(principal.getUser().getId(), request.getAmount());
        return ResponseResult.successWithMessage("예치금 충전 신청이 완료되었습니다.");
    }

    @Operation(summary = "내 충전 신청 내역 조회")
    @GetMapping("/charges")
    public ResponseEntity<?> getMyChargeRequests(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("충전 신청 내역 조회가 완료되었습니다.",
                chargeService.getMyChargeRequests(principal.getUser().getId()));
    }

    private Long requireStoreId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getStore() == null) {
            throw new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED");
        }
        return principal.getUser().getStore().getId();
    }
}
