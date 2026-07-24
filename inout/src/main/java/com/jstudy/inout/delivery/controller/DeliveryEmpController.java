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
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.delivery.dto.DeliveryDto;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.service.DeliveryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "?? ?? ??", description = "?? ?? ?? ?? ?? ?? ?? (EMPLOYEE / OWNER / ADMIN)")
@RestController
@RequestMapping("/api/emp/deliveries")
@PreAuthorize("hasAnyRole('EMPLOYEE', 'OWNER', 'ADMIN')")
@RequiredArgsConstructor
public class DeliveryEmpController {

    private final DeliveryService deliveryService;

    @Operation(summary = "? ?? ?? ??", description = "??? ??? ??? ?? ??? ??? ?????.")
    @GetMapping
    public ResponseEntity<?> getMyDeliveryList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(name = "status", required = false) DeliveryStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("?? ??? ???? ????.", 401, "UNAUTHORIZED");
        }

        Page<DeliveryDto.ListItem> result =
                deliveryService.getMyDeliveryList(principal.getUser().getId(), status, pageable);

        return ResponseResult.success("?? ?? ??? ???????.", result);
    }
}
