package com.jstudy.inout.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.payment.dto.PaymentDto;
import com.jstudy.inout.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제", description = "예치금으로 발주 대금을 결제합니다 (EMPLOYEE / ADMIN)")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "예치금 결제",
               description = """
                       로그인 직원의 예치금 계좌에서 발주 대금을 차감합니다.
                       
                       **처리 흐름:**
                       1. 주문 행 비관적 락(PESSIMISTIC_WRITE) 획득 — 이중 결제 방지
                       2. 예치금 계좌 비관적 락 획득 — 잔액 경쟁 방지
                       3. 금액 일치 검증 (`equals` 값 비교)
                       4. 잔액 차감 + 이력 기록
                       5. 주문 상태 **REQUESTED → PAID** 변경
                       
                       **주의:** `amount` 값은 반드시 실제 주문 금액과 일치해야 합니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 성공 — 남은 잔액 반환"),
            @ApiResponse(responseCode = "400", description = "결제 금액 불일치 / REQUESTED 상태가 아닌 주문"),
            @ApiResponse(responseCode = "403", description = "타인의 주문 결제 시도"),
            @ApiResponse(responseCode = "404", description = "주문 없음 / 예치금 계좌 없음"),
            @ApiResponse(responseCode = "422", description = "예치금 잔액 부족")
    })
    @PostMapping("/deposit")
    public ResponseEntity<?> payWithDeposit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentDto.Request request) {

        PaymentDto.Response responseData = paymentService.processDepositPayment(
                userDetails.getUser().getId(),
                request
        );
        return ResponseResult.success("예치금 결제가 성공적으로 처리되었습니다.", responseData);
    }
}
