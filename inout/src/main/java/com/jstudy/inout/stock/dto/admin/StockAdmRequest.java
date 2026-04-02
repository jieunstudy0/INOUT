package com.jstudy.inout.stock.dto.admin;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StockAdmRequest {

	private Long itemId;
    private int actualStock; 
    private String reason;
}
