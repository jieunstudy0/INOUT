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

@Tag(name = "결제", description = "레거시 예치금 결제 (프랜차이즈 3단계에서는 점주 승인 API 사용)")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "예치금 결제 (비활성)",
               description = """
                       직원 직접 결제는 더 이상 지원하지 않습니다.
                       직원 기안(REQUESTED)은 점주 `PATCH /api/owner/orders/{id}/modify-and-approve` 로만 결제·ORDERED 전환됩니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "OWNER_APPROVAL_REQUIRED — 점주 승인·결제 필요"),
            @ApiResponse(responseCode = "404", description = "주문 없음")
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
