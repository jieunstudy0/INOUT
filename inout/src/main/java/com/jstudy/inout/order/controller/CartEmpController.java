package com.jstudy.inout.order.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.order.dto.CartAddRequest;
import com.jstudy.inout.order.dto.CartResponse;
import com.jstudy.inout.order.service.CartEmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order/emp")
@RequiredArgsConstructor
public class CartEmpController {

    private final CartEmpService cartEmpService;

    @PostMapping("/cart")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> addToCart(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CartAddRequest request
    ) {
        Long currentUserId = principal.getUser().getId();
        cartEmpService.addToCart(currentUserId, request);
        return ResponseEntity.ok(ResponseResult.success("상품이 장바구니에 담겼습니다."));
    }

    @GetMapping("/cart")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getCartList(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {	
        Long currentUserId = principal.getUser().getId();
        CartResponse cartResponse = cartEmpService.getCartList(currentUserId);
        return ResponseEntity.ok(ResponseResult.success("장바구니 조회가 완료되었습니다.", cartResponse));
    }
    
    @DeleteMapping("/cart/items")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> deleteSelectedItems(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody List<Long> cartDetailIds
    ) {
        cartEmpService.deleteSelectedCartItems(principal.getUser().getId(), cartDetailIds);
        return ResponseEntity.ok(ResponseResult.success("선택한 상품이 삭제되었습니다."));
    }

    @DeleteMapping("/cart")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> deleteAllItems(@AuthenticationPrincipal CustomUserDetails principal) {
        cartEmpService.deleteAllCartItems(principal.getUser().getId());
        return ResponseEntity.ok(ResponseResult.success("장바구니가 비워졌습니다."));
    }

    @PostMapping("/cart/reorder/{orderId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> reOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long orderId
    ) {
        Long currentUserId = principal.getUser().getId();
        cartEmpService.reOrder(currentUserId, orderId);
        return ResponseEntity.ok(ResponseResult.success("과거 주문 상품이 장바구니에 담겼습니다."));
    }
}