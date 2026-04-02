package com.jstudy.inout.order.dto;

import java.util.List;
import java.util.stream.Collectors;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.order.entity.CartDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderPreResponse {

    private String storeName;
    
    private String employeeName;
    
    private String storeAddress;
    
    private List<OrderItemDto> items;
    
    private Integer totalQuantity;
    
    private Long totalPrice;

    public static OrderPreResponse from(User user, List<CartDetail> cartDetails) {
        return OrderPreResponse.builder()
                .storeName(user.getStore().getName()) 
                .employeeName(user.getName())
                .storeAddress(user.getStore().getAddress())
                .items(cartDetails.stream().map(OrderItemDto::from).collect(Collectors.toList()))
                .totalQuantity(cartDetails.stream().mapToInt(CartDetail::getQuantity).sum())
                .totalPrice(cartDetails.stream().mapToLong(cd -> (long) cd.getQuantity() * cd.getItem().getUnitPrice()).sum())
                .build();
    }
}
