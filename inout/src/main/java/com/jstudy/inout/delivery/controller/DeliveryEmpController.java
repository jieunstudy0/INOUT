package com.jstudy.inout.delivery.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.service.DeliveryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "직원 배송 조회", description = "본인이 신청한 발주의 배송 현황 조회 (EMPLOYEE / OWNER / ADMIN)")
@RestController
@RequestMapping("/api/emp/deliveries")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
@RequiredArgsConstructor
public class DeliveryEmpController {

    private final DeliveryService deliveryService;

    @Operation(summary = "내 배송 목록 조회", description = "본인이 신청한 발주의 배송 현황을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getMyDeliveryList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "status", required = false) DeliveryStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException(ErrorCode.UNAUTHORIZED);
        }

        Page<DeliveryDto.ListItem> result =
                deliveryService.getMyDeliveryList(principal.getUser().getId(), status, pageable);

        return ResponseResult.success("배송 목록 조회가 완료되었습니다.", result);
    }
}
