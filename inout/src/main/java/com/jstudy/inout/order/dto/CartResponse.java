package com.jstudy.inout.order.dto;

import java.util.List;
import com.jstudy.inout.order.entity.CartDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartResponse {
    private List<CartItemResponse> items; 
    private Integer totalQuantity;
    private Long totalPrice; 

    @Getter
    @Builder
    public static class CartItemResponse {
        private Long cartId; 
        private String itemName;    
        private Integer quantity;    
        private Long unitPrice; 
        private Long subTotal; 
        
        public static CartItemResponse from(CartDetail cartDetail) {
            return CartItemResponse.builder()
                    .cartId(cartDetail.getCartDetailId())
                    .itemName(cartDetail.getItem().getName())
                    .quantity(cartDetail.getQuantity())
                    .unitPrice(cartDetail.getItem().getUnitPrice())
                    .subTotal((long) cartDetail.getItem().getUnitPrice() * cartDetail.getQuantity())
                    .build();
        }
    }
}
