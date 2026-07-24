package com.jstudy.inout.delivery.controller;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.service.DeliveryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가맹점주 배송 조회", description = "소속 매장 전체 배송 현황 조회 (OWNER 전용, 상태 변경 불가)")
@RestController
@RequestMapping("/api/owner/deliveries")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
public class DeliveryOwnerController {

    private final DeliveryService deliveryService;

    @Operation(summary = "매장 배송 목록 조회",
               description = "소속 매장 직원들이 신청한 발주의 배송 현황을 조회합니다. 배송 시작/완료는 본사(ADMIN)만 가능합니다.")
    @GetMapping
    public ResponseEntity<?> getStoreDeliveryList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "status", required = false) DeliveryStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long storeId = requireStoreId(principal);
        Page<DeliveryDto.ListItem> result = deliveryService.getStoreDeliveryList(storeId, status, pageable);
        return ResponseResult.success("매장 배송 목록 조회가 완료되었습니다.", result);
    }

    private Long requireStoreId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getStore() == null) {
            throw new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED");
        }
        return principal.getUser().getStore().getId();
    }
}
