package com.jstudy.inout.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.OrderCreateRequest;
import com.jstudy.inout.order.dto.OrderPreResponse;
import com.jstudy.inout.order.service.OrderEmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "직원 발주 관리", description = "발주 미리보기, 신청, 이력 조회, 취소 (EMPLOYEE / ADMIN)")
@RestController
@RequestMapping("/api/emp/orders")
@RequiredArgsConstructor
public class OrderEmpController {

    private final OrderEmpService orderEmpService;

    @Operation(summary = "발주 미리보기",
               description = "장바구니에서 선택한 품목으로 발주 금액·수신자 정보를 사전 확인합니다. DB에 저장하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "미리보기 데이터 반환"),
            @ApiResponse(responseCode = "400", description = "선택 항목 없음"),
            @ApiResponse(responseCode = "403", description = "타인의 장바구니 항목 포함")
    })
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getOrderPreview(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderPreResponse previewResponse = orderEmpService.getOrderPreview(getUserId(principal), request);
        return ResponseResult.success("발주 미리보기를 불러왔습니다.", previewResponse);
    }

    @Operation(summary = "발주 신청",
               description = """
                       장바구니에서 선택한 품목으로 발주 기안을 생성합니다.
                       - 발주 상태: **REQUESTED** (점주 승인 대기, 예치금 미차감)
                       - 예치금 결제는 점주 `modify-and-approve` 에서만 수행됩니다.
                       - 수신자·주소 미입력 시 직원 기본 정보로 자동 채워집니다.
                       - 성공 시 해당 장바구니 항목은 논리 삭제됩니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발주 기안 성공 (주문 ID 반환)"),
            @ApiResponse(responseCode = "400", description = "선택 항목 없음 / 재고 부족"),
            @ApiResponse(responseCode = "403", description = "타인의 장바구니 항목 포함")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> submitOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid OrderCreateRequest request) {

        Long orderId = orderEmpService.submitOrderRequest(getUserId(principal), request);

        return ResponseResult.success("발주 기안이 등록되었습니다. 점주 승인 대기 중입니다.", orderId);
    }

    @Operation(summary = "내 발주 이력 목록 조회",
               description = "로그인한 직원의 발주 목록(기안·결제·승인 포함)을 최신순으로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getMyOrderHistory(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("발주 이력 조회가 완료되었습니다.",
                orderEmpService.getMyOrderHistory(getUserId(principal)));
    }

    @Operation(summary = "발주 상세 조회",
               description = "특정 발주의 품목별 상세 정보(단가 스냅샷 포함)를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "타인의 발주 조회 시도"),
            @ApiResponse(responseCode = "404", description = "발주 없음")
    })
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getOrderDetail(
            @Parameter(description = "조회할 발주 ID") @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseResult.success("발주 상세 조회가 완료되었습니다.",
                orderEmpService.getOrderDetails(getUserId(principal), orderId));
    }

    @Operation(summary = "발주 취소",
               description = """
                       발주를 취소합니다. 취소 가능 상태 및 처리 방식은 아래와 같습니다.
                       - **REQUESTED(결제 대기)**: 예치금 차감 전이므로 상태만 CANCELLED로 변경합니다.
                       - **PAID(결제 완료)**: 관리자 처리 전에 한해 취소 가능하며, 결제된 예치금이 **즉시 전액 환불**됩니다.
                       - PARTIAL / COMPLETED / REJECTED / CANCELLED 상태는 취소할 수 없습니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공 (PAID 상태인 경우 예치금 환불 포함)"),
            @ApiResponse(responseCode = "400", description = "취소 불가 상태 (PARTIAL · COMPLETED · REJECTED · CANCELLED)"),
            @ApiResponse(responseCode = "403", description = "타인의 발주 취소 시도"),
            @ApiResponse(responseCode = "404", description = "발주 없음")
    })
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> cancelOrder(
            @Parameter(description = "취소할 발주 ID") @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        orderEmpService.cancelOrder(getUserId(principal), orderId);
        return ResponseResult.successWithMessage("발주가 정상적으로 취소되었습니다.");
    }

    private Long getUserId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("인증 정보가 유효하지 않습니다.", 401, "UNAUTHORIZED");
        }
        return principal.getUser().getId();
    }
}