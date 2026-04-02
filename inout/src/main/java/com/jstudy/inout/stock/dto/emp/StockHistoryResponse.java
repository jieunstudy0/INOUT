package com.jstudy.inout.stock.dto.emp;

import java.time.LocalDateTime;
import com.jstudy.inout.stock.entity.StockReceivingHistory;
import com.jstudy.inout.stock.entity.StockUsageHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockHistoryResponse {
    
    private Long historyId;
    
    private String itemName;    
    
    private String type;    

    private Integer quantity; 
    
    private LocalDateTime date;  

    private String status;    
    
    private String remarks;   

    private String workerName;  
    
    private Integer resultStock;  

    public static StockHistoryResponse from(StockReceivingHistory entity) {
        return StockHistoryResponse.builder()
                .historyId(entity.getHistoryId())
                .itemName(entity.getItem().getName())
                .type("입고")
                .quantity(entity.getReceivingQuantity())
                .date(entity.getProcessDate())
                .status("완료")
                .remarks(entity.getMemo())
                .workerName(entity.getUser().getName())
                .resultStock(entity.getResultStock())
                .build();
    }

    public static StockHistoryResponse from(StockUsageHistory entity) {
        return StockHistoryResponse.builder()
                .historyId(entity.getHistoryId())
                .itemName(entity.getItem().getName())
                .type("사용")
                .quantity(entity.getUsageQuantity()) 
                .date(entity.getProcessDate())
                .status("완료")
                .remarks(entity.getMemo())
                .workerName(entity.getUser().getName())
                .resultStock(entity.getResultStock())
                .build();
    }
}
