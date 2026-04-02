package com.jstudy.inout.order.dto;

import java.time.LocalDateTime;
import com.jstudy.inout.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAdminResponse {
    private Long orderRequestId;
    private String storeName;        
    private String employeeName;    
    private LocalDateTime requestDate;
    private OrderStatus status;    
    private Long totalPrice;
    private String representativeItemName; 
    private Integer itemCount; 
}
