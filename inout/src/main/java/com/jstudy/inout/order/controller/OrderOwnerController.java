package com.jstudy.inout.order.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.order.dto.OrderAdminDetailResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.dto.OrderRejectRequest;
import com.jstudy.inout.order.dto.OwnerOrderCreateRequest;
import com.jstudy.inout.order.dto.OwnerOrderModifyRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.service.OrderOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "가맹점주 발주", description = "직원 기안 검토·결제, 직접 발주, 매장 발주 조회 (OWNER 전용)")
@RestController
@RequestMapping("/api/owner/orders")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OrderOwnerController {

    private final OrderOwnerService orderOwnerService;

    @Operation(summary = "매장 발주 목록 조회")
    @GetMapping
    public ResponseEntity<?> getStoreOrders(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "requestDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<OrderAdminResponse> result =
                orderOwnerService.getStoreOrders(principal.getUser().getId(), status, pageable);
        return ResponseResult.success("매장 발주 목록 조회가 완료되었습니다.", result);
    }

    @Operation(summary = "점주 직접 발주", description = "품목 지정 후 즉시 예치금 결제하여 ORDERED 상태로 생성합니다.")
    @PostMapping
    public ResponseEntity<?> createOwnerOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OwnerOrderCreateRequest request) {

        Long orderId = orderOwnerService.createOwnerOrder(principal.getUser().getId(), request);
        return ResponseResult.success("점주 발주가 완료되었습니다.", orderId);
    }

    @Operation(summary = "매장 발주 상세 조회")
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getStoreOrderDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("orderId") Long orderId) {

        OrderAdminDetailResponse detail =
                orderOwnerService.getStoreOrderDetail(principal.getUser().getId(), orderId);
        return ResponseResult.success("매장 발주 상세 조회가 완료되었습니다.", detail);
    }

    @Operation(summary = "직원 기안 수정 및 결제 승인",
               description = "REQUESTED 발주의 품목·수량을 수정한 뒤 예치금을 차감하고 ORDERED로 전환합니다.")
    @PatchMapping("/{orderId}/modify-and-approve")
    public ResponseEntity<?> modifyAndApprove(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody OwnerOrderModifyRequest request) {

        Long id = orderOwnerService.modifyAndApprove(principal.getUser().getId(), orderId, request);
        return ResponseResult.success("기안 수정 및 결제 승인이 완료되었습니다.", id);
    }

    @Operation(summary = "직원 기안 반려", description = "미결제(REQUESTED) 기안을 REJECTED로 반려합니다. 예치금 차감이 없어 환불은 없습니다.")
    @PatchMapping("/{orderId}/reject")
    public ResponseEntity<?> rejectDraft(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("orderId") Long orderId,
            @RequestBody(required = false) OrderRejectRequest request) {

        orderOwnerService.rejectDraft(principal.getUser().getId(), orderId, request);
        return ResponseResult.success("기안이 반려되었습니다.", null);
    }
}
