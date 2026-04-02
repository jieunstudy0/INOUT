package com.jstudy.inout.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.jstudy.inout.order.service.OrderEmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order/emp")
@RequiredArgsConstructor
public class OrderEmpController {

    private final OrderEmpService orderEmpService;

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        Long currentUserId = getUserIdFromUserDetails(principal);
        orderEmpService.getOrderPreview(currentUserId, request);
        
        return ResponseEntity.ok(ResponseResult.success("발주 요청이 완료되었습니다."));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") 
    public ResponseEntity<?> submitOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid OrderCreateRequest request) {
        
        Long currentUserId = getUserIdFromUserDetails(principal);
        orderEmpService.submitOrderRequest(currentUserId, request);

        return ResponseEntity.ok(ResponseResult.success("발주 요청이 정상적으로 등록되었습니다."));
    }

    private Long getUserIdFromUserDetails(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("인증 정보가 유효하지 않습니다.", 401, "UNAUTHORIZED");
        }
        return principal.getUser().getId();
    }
    
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long currentUserId = getUserIdFromUserDetails(principal); 
        orderEmpService.cancelOrder(currentUserId, orderId);

        return ResponseEntity.ok(ResponseResult.success("발주가 정상적으로 취소되었습니다."));
    }
    
}
