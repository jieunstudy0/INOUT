package com.jstudy.inout.stock.dto.admin;

import java.time.LocalDateTime;
import com.jstudy.inout.stock.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StockAdminResponse {

    private Long itemId;
    
    private String categoryName;
    
    private String name;
    
    private Long unitPrice;
    
    private Integer currentStock;
    
    private Integer minStockLevel;
    
    private String unitDescription;
    
    private Boolean deleted; 
    
    private LocalDateTime createdAt;

    public static StockAdminResponse from(Item item) {
        return StockAdminResponse.builder()
                .itemId(item.getItemId())
                .categoryName(item.getCategory() != null ? item.getCategory().getCategoryName() : "미지정") // Null 방어 코드 추가
                .name(item.getName())
                .unitPrice(item.getUnitPrice())
                .currentStock(item.getCurrentStock())
                .minStockLevel(item.getMinStockLevel())
                .unitDescription(item.getUnitDescription()) 
                .deleted(item.getDeleted())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
