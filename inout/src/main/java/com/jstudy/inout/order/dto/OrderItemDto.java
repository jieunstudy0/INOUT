package com.jstudy.inout.order.dto;

import com.jstudy.inout.order.entity.CartDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OrderItemDto {
    private Long cartDetailId;  
    private String itemName;    
    private Integer quantity;   
    private Long unitPrice;  
    private Long subTotal; 

    public static OrderItemDto from(CartDetail cartDetail) {
        return OrderItemDto.builder()
                .cartDetailId(cartDetail.getCartDetailId())
                .itemName(cartDetail.getItem().getName())
                .quantity(cartDetail.getQuantity())
                .unitPrice(cartDetail.getItem().getUnitPrice())
                .subTotal((long) cartDetail.getQuantity() * cartDetail.getItem().getUnitPrice())
                .build();
    }
}
