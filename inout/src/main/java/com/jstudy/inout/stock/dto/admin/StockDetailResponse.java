package com.jstudy.inout.stock.dto.admin;

import java.util.List;
import com.jstudy.inout.stock.dto.emp.StockHistoryResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockDetailResponse {

    private Long itemId;
    
    private String itemName;
    
    private String categoryName;
    
    private Integer currentStock;
    
    private Integer minStockLevel;
    
    private String status;

    private List<StockHistoryResponse> history;

    private Long totalReceived; 
    
    private Long totalUsed;    
}
