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
import com.jstudy.inout.order.dto.OrderCreateRequest;
import com.jstudy.inout.order.service.CartEmpService;
import com.jstudy.inout.order.service.OrderEmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/order/emp")
@RequiredArgsConstructor
public class CartEmpController {

    private final OrderEmpService orderEmpService;
    private final CartEmpService cartEmpService;

    @PostMapping("/cart")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> addToCart(
    		@AuthenticationPrincipal CustomUserDetails principal, // 로그인한 사용자 정보
            @Valid @RequestBody CartAddRequest request
    ) {
    	
    	Long currentUserId = principal.getUser().getId();
    	
    	cartEmpService.addToCart(currentUserId, request);
        return ResponseResult.success("상품이 장바구니에 담겼습니다.", null);
    }

    @GetMapping("/cart")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getCartList(
    		@AuthenticationPrincipal CustomUserDetails principal
    ) {	
    	Long currentUserId = principal.getUser().getId();
    	    
        CartResponse cartResponse = cartEmpService.getCartList(currentUserId);
        return ResponseResult.success("장바구니 조회가 완료되었습니다.", cartResponse);
    }
    
    @DeleteMapping("/cart/items")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> deleteSelectedItems(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody List<Long> cartDetailIds // 삭제할 ID 리스트
    ) {
    	cartEmpService.deleteSelectedCartItems(principal.getUser().getId(), cartDetailIds);
        return ResponseResult.success("선택한 상품이 삭제되었습니다.", null);
    }

    @DeleteMapping("/cart")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> deleteAllItems(@AuthenticationPrincipal CustomUserDetails principal) {
    	cartEmpService.deleteAllCartItems(principal.getUser().getId());
        return ResponseResult.success("장바구니가 비워졌습니다.", null);
    }
    
    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        Long currentUserId = principal.getUser().getId();
        orderEmpService.getOrderPreview(currentUserId, request);
        
        return ResponseResult.success("발주 요청이 완료되었습니다.", null);
    }

    @PostMapping("/cart/reorder/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> reOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long orderId
    ) {
        Long currentUserId = principal.getUser().getId();
        
        cartEmpService.reOrder(currentUserId, orderId);
        
        return ResponseResult.success("과거 주문 상품이 장바구니에 담겼습니다.");
    }
}
