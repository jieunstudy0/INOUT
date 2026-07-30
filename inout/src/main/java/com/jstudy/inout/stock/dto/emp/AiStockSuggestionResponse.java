package com.jstudy.inout.stock.dto.emp;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiStockSuggestionResponse {
    private Long itemId;
    private String itemName;
    private Integer currentStock;
    private Integer minStockLevel;
    private Integer recommendQty;
    private String reason;
    private Long unitPrice;
    private Long recentSalesQty;
}
