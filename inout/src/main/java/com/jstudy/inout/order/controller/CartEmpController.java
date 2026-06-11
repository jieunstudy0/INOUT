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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.order.dto.CartAddRequest;
import com.jstudy.inout.order.dto.CartQuantityUpdateRequest;
import com.jstudy.inout.order.dto.CartResponse;
import com.jstudy.inout.order.service.CartEmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "직원 장바구니", description = "상품 담기, 수량 변경, 삭제, 재주문 (EMPLOYEE / ADMIN)")
@RestController
@RequestMapping("/api/emp/carts")
@RequiredArgsConstructor
public class CartEmpController {

    private final CartEmpService cartEmpService;

    @Operation(summary = "장바구니에 상품 담기",
               description = "상품 ID와 수량을 입력해 장바구니에 추가합니다. 이미 담긴 상품이면 수량이 합산됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "담기 성공"),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> addToCart(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CartAddRequest request
    ) {
        cartEmpService.addToCart(principal.getUser().getId(), request);
        return ResponseResult.successWithMessage("상품이 장바구니에 담겼습니다.");
    }

    @Operation(summary = "장바구니 목록 조회",
               description = "로그인 직원의 장바구니 전체 목록(품목별 소계, 합계)을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> getCartList(@AuthenticationPrincipal CustomUserDetails principal) {
        CartResponse cartResponse = cartEmpService.getCartList(principal.getUser().getId());
        return ResponseResult.success("장바구니 조회가 완료되었습니다.", cartResponse);
    }

    @Operation(summary = "장바구니 항목 수량 변경",
               description = "cartDetailId에 해당하는 항목의 수량을 변경합니다. 수량은 1 이상이어야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
            @ApiResponse(responseCode = "400", description = "수량이 1 미만"),
            @ApiResponse(responseCode = "403", description = "타인의 장바구니 항목 수정 시도"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목 없음")
    })
    @PatchMapping("/{cartDetailId}/quantity")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "수량을 변경할 장바구니 항목 ID") @PathVariable Long cartDetailId,
            @Valid @RequestBody CartQuantityUpdateRequest request
    ) {
        cartEmpService.updateQuantity(principal.getUser().getId(), cartDetailId, request.getQuantity());
        return ResponseResult.successWithMessage("수량이 변경되었습니다.");
    }

    @Operation(summary = "선택 상품 삭제",
               description = "cartDetailId 목록에 해당하는 상품을 장바구니에서 삭제합니다 (논리 삭제).")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/items")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> deleteSelectedItems(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody List<Long> cartDetailIds
    ) {
        cartEmpService.deleteSelectedCartItems(principal.getUser().getId(), cartDetailIds);
        return ResponseResult.successWithMessage("선택한 상품이 삭제되었습니다.");
    }

    @Operation(summary = "장바구니 전체 비우기")
    @ApiResponse(responseCode = "200", description = "전체 삭제 성공")
    @DeleteMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> deleteAllItems(@AuthenticationPrincipal CustomUserDetails principal) {
        cartEmpService.deleteAllCartItems(principal.getUser().getId());
        return ResponseResult.successWithMessage("장바구니가 비워졌습니다.");
    }

    @Operation(summary = "과거 주문 재주문",
               description = "이전 발주의 품목을 장바구니에 다시 담습니다. 삭제되었거나 품절된 상품은 건너뜁니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재주문 담기 성공"),
            @ApiResponse(responseCode = "403", description = "타인의 발주 재주문 시도"),
            @ApiResponse(responseCode = "404", description = "발주 없음")
    })
    @PostMapping("/reorder/{orderId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> reOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "재주문 기준 발주 ID") @PathVariable Long orderId
    ) {
        cartEmpService.reOrder(principal.getUser().getId(), orderId);
        return ResponseResult.successWithMessage("과거 주문 상품이 장바구니에 담겼습니다.");
    }
}
