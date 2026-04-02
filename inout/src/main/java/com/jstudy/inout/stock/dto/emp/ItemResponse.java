package com.jstudy.inout.stock.dto.emp;

import com.jstudy.inout.stock.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ItemResponse {
    private Long itemId;
    private String categoryName;    
    private String name;  
    private Long unitPrice; 
    private Integer currentStock;  
    private String status; 
    private String unitDescription;

    public static ItemResponse from(Item item) { 
        return ItemResponse.builder()
                .itemId(item.getItemId())
                .categoryName(item.getCategory().getCategoryName())
                .name(item.getName())
                .unitPrice(item.getUnitPrice())
                .currentStock(item.getCurrentStock())
                .status(determineStatus(item))
                .unitDescription(item.getUnitDescription())
                .build();
    }

    private static String determineStatus(Item item) {
        if (item.getCurrentStock() <= 0) return "품절";
        if (item.getCurrentStock() < item.getMinStockLevel()) return "재고부족";
        return "정상";
    }
}
