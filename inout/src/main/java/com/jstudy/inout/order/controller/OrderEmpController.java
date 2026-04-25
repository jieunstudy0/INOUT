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
import com.jstudy.inout.order.dto.OrderPreResponse;
import com.jstudy.inout.order.service.OrderEmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order/emp")
@RequiredArgsConstructor
public class OrderEmpController {

    private final OrderEmpService orderEmpService;

    // 💡 /request 중복을 피하고, 역할을 명확히 하기 위해 /preview로 변경
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getOrderPreview(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        Long currentUserId = getUserIdFromUserDetails(principal);
        // 응답값을 반환하도록 수정 (미리보기 데이터를 클라이언트에 전달해야 함)
        OrderPreResponse previewResponse = orderEmpService.getOrderPreview(currentUserId, request);
        return ResponseEntity.ok(ResponseResult.success("발주 미리보기를 불러왔습니다.", previewResponse));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')") 
    public ResponseEntity<?> submitOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid OrderCreateRequest request) {
        
        Long currentUserId = getUserIdFromUserDetails(principal);
        orderEmpService.submitOrderRequest(currentUserId, request);
        return ResponseEntity.ok(ResponseResult.success("발주 요청이 정상적으로 등록되었습니다."));
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long currentUserId = getUserIdFromUserDetails(principal); 
        orderEmpService.cancelOrder(currentUserId, orderId);
        return ResponseEntity.ok(ResponseResult.success("발주가 정상적으로 취소되었습니다."));
    }

    private Long getUserIdFromUserDetails(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InoutException("인증 정보가 유효하지 않습니다.", 401, "UNAUTHORIZED");
        }
        return principal.getUser().getId();
    }
}