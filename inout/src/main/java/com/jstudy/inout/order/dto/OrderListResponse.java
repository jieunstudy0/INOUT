package com.jstudy.inout.order.dto;

import java.time.LocalDateTime;
import com.jstudy.inout.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class OrderListResponse {
	
	 private Long orderRequestId;     
	 
	 private LocalDateTime requestDate; 
	 
	 private OrderStatus status; 
	 
	 private Long totalPrice;    
	 
	 private int itemCount;         
	 
	 private String representativeItemName; 
	 
}

