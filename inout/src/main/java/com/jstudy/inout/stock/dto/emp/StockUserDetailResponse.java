package com.jstudy.inout.stock.dto.emp;

import com.jstudy.inout.stock.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StockUserDetailResponse {
	
    private Long itemId;
    
    private String name;     
    
    private String categoryName;
    
    private Integer currentStock;
    
    private String unitDescription;
    
    private Long unitPrice; 
    
    private String description;    
    
    private String status;  
    
    public static StockUserDetailResponse from(Item item) {
        String status = "정상";
        if (item.getCurrentStock() == 0) status = "품절";
        else if (item.getCurrentStock() <= item.getMinStockLevel()) status = "저재고";

        return StockUserDetailResponse.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .categoryName(item.getCategory() != null ? item.getCategory().getCategoryName() : "미지정")
                .currentStock(item.getCurrentStock())
                .unitDescription(item.getUnitDescription())
                .unitPrice(item.getUnitPrice())
                .description(item.getDescription())
                .status(status)
                .build();
    }
}
