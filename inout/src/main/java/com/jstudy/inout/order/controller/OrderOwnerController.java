package com.jstudy.inout.order.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.order.dto.OrderAdminDetailResponse;
import com.jstudy.inout.order.dto.OrderAdminResponse;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.service.OrderOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가맹점주 발주 조회", description = "소속 매장 발주 내역 조회 (OWNER 전용, 읽기 전용)")
@RestController
@RequestMapping("/api/owner/orders")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class OrderOwnerController {

    private final OrderOwnerService orderOwnerService;

    @Operation(summary = "매장 발주 목록 조회",
               description = "소속 매장 직원들이 본사로 요청한 발주 내역을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getStoreOrders(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "requestDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<OrderAdminResponse> result =
                orderOwnerService.getStoreOrders(principal.getUser().getId(), status, pageable);
        return ResponseResult.success("매장 발주 목록 조회가 완료되었습니다.", result);
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
}
