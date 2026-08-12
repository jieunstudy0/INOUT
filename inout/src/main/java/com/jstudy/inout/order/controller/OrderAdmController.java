package com.jstudy.inout.order.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.order.dto.BulkOrderRequest;
import com.jstudy.inout.order.dto.BulkOrderResponse;
import com.jstudy.inout.order.dto.OrderAdminDetailResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.dto.OrderProcessRequest;
import com.jstudy.inout.order.dto.OrderRejectRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.service.OrderAdmService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import com.jstudy.inout.common.exception.InoutException;

@Tag(name = "관리자 발주 관리", description = "결제 완료된 발주 건 승인·반려, 일괄 승인, 엑셀 다운로드 (ADMIN 전용)")
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class OrderAdmController {

    private final OrderAdmService orderAdmService;

    @Operation(summary = "발주 목록 조회",
               description = "전체 발주 목록을 최신순으로 조회합니다. `status` 파라미터로 상태별 필터링이 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    @GetMapping
    public ResponseEntity<?> getAllOrderRequests(
            @Parameter(description = "발주 상태 필터 (ORDERED·APPROVED·REJECTED 등). REQUESTED(직원 기안)는 본사 목록에서 제외됩니다. 생략 시 점주 결제 완료 건만 조회")
            @RequestParam(name = "status", required = false) OrderStatus status) {

        List<OrderAdminResponse> orders = orderAdmService.getAllOrders(status);
        return ResponseResult.success("발주 목록 조회가 완료되었습니다.", orders);
    }

    @Operation(summary = "본사 최종 승인", description = "ORDERED 발주를 APPROVED로 전환하고 재고 차감·배송을 생성합니다.")
    @PatchMapping("/{orderId}/approve")
    public ResponseEntity<?> approveOrder(
            @PathVariable(name = "orderId") Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        orderAdmService.approveOrder(orderId, principal.getUser().getId());
        return ResponseResult.success("본사 최종 승인이 완료되었습니다.", null);
    }

    @Operation(summary = "본사 발주 반려", description = "ORDERED 발주를 REJECTED로 전환하고 차감된 예치금을 환불합니다.")
    @PatchMapping("/{orderId}/reject")
    public ResponseEntity<?> rejectOrder(
            @PathVariable(name = "orderId") Long orderId,
            @RequestBody(required = false) OrderRejectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        orderAdmService.rejectOrder(orderId, principal.getUser().getId(), request);
        return ResponseResult.success("발주가 반려되고 예치금이 환불되었습니다.", null);
    }

    @Operation(summary = "발주 상세 조회",
               description = "발주 메타 정보와 품목별 상세(orderDetailId, 상태 포함)를 반환합니다. 프론트 모달 및 개별 처리 버튼에 사용됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "발주 없음")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(
            @Parameter(description = "조회 대상 발주 ID") @PathVariable(name = "orderId") Long orderId) {
        OrderAdminDetailResponse detail = orderAdmService.getOrderDetail(orderId);
        return ResponseResult.success("발주 상세 조회가 완료되었습니다.", detail);
    }

    @Operation(summary = "발주 상세 항목별 처리",
               description = """
                       발주 내 개별 품목을 승인(APPROVED)·반려(REJECTED)·지연(DELAYED) 처리합니다.
                       - 승인 시 비관적 락(PESSIMISTIC_WRITE)으로 재고를 차감합니다.
                       - 전체 품목이 처리 완료되면 주문 상태가 **COMPLETED**, 미처리 항목이 있으면 **PARTIAL**이 됩니다.
                       - COMPLETED 전환 시 배송 엔티티가 자동 생성됩니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "미결제 주문 처리 시도 / 빈 요청 / 잘못된 상태 전이"),
            @ApiResponse(responseCode = "404", description = "주문 없음 / 상세 항목 없음")
    })
    @PatchMapping("/{orderId}/process")
    public ResponseEntity<?> processOrderDetail(
            @Parameter(description = "처리 대상 발주 ID") @PathVariable(name = "orderId") Long orderId,
            @RequestBody OrderProcessRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        orderAdmService.processOrderItems(orderId, request, principal.getUser().getId());
        return ResponseResult.success("발주 상세 처리가 완료되었습니다.", null);
    }

    @Operation(summary = "발주 일괄 승인",
               description = """
                       여러 발주를 한 번에 승인합니다. 각 발주는 **독립 트랜잭션(REQUIRES_NEW)**으로 처리되어
                       하나가 재고 부족으로 실패해도 나머지는 정상 승인됩니다.
                       재고 부족 발주는 자동으로 **REJECTED** 처리되고 예치금이 전액 환불됩니다.
                       응답에는 성공·자동반려·실패 건수와 실패 상세 목록이 포함됩니다.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 완료 (부분 실패 포함 가능, 응답 body 확인)"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    @PostMapping("/bulk-approve")
    public ResponseEntity<?> bulkApprove(
            @RequestBody BulkOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        BulkOrderResponse response = orderAdmService.bulkApproveOrders(request, principal.getUser().getId());
        String message = String.format(
                "%d건 승인, %d건 재고부족 자동반려",
                response.getSuccessCount(),
                response.getAutoRejectCount());
        return ResponseResult.success(message, response);
    }

    @Operation(summary = "발주 내역 엑셀 다운로드",
               description = "전체 발주 목록을 `발주내역리스트_YYYYMMDD.xlsx` 파일로 다운로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 다운로드 성공"),
            @ApiResponse(responseCode = "500", description = "엑셀 생성 중 서버 오류")
    })
    @GetMapping("/excel")
    public void downloadExcel(HttpServletResponse response) {
        try {
            orderAdmService.exportOrdersToExcel(response);
        } catch (IOException e) {
            throw new InoutException("엑셀 파일 생성 중 오류가 발생했습니다.", 500, "EXCEL_ERROR");
        }
    }
}
