package com.jstudy.inout.order.dto;

import java.util.List;
import com.jstudy.inout.order.entity.OrderDetailStatus; 

public record OrderProcessRequest(
		
	 List<ItemStatusUpdate> items
	) {

	 public record ItemStatusUpdate(
	     Long orderDetailId,     
	     OrderDetailStatus status 
	 ) {}
}
