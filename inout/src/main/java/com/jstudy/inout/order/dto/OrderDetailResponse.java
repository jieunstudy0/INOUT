package com.jstudy.inout.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.jstudy.inout.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderDetailResponse {
	
	private Long orderRequestId;
	
	private LocalDateTime requestDate;
	
	private OrderStatus status;
	
	private String storeName;    
	
 	private String employeeName;    
 
 	private List<OrderDetailItemDto> items;
 	
 	private Long totalPrice;

	@Getter
	@Builder
	public static class OrderDetailItemDto {
	    private String itemName;
	    private Integer quantity;
	    private Long priceSnapshot;
	    private Long subTotal;
	 }
}
