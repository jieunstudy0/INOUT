package com.jstudy.inout.stock.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder 
@AllArgsConstructor 
@NoArgsConstructor
public class StockAdmRequest {

	private Long itemId;
    private int actualStock; 
    private String reason;
}
