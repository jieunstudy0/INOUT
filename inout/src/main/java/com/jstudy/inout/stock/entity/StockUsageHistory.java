package com.jstudy.inout.stock.entity;

import java.time.LocalDateTime;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Table(name = "stock_usage_history")
@SuperBuilder
@NoArgsConstructor
@AttributeOverrides({
    @AttributeOverride(name = "processDate", column = @Column(name = "usage_date")),
    @AttributeOverride(name = "user", column = @Column(name = "usage_user_id")) 
})
public class StockUsageHistory extends BaseStockHistory {
    private Integer usageQuantity;
    
    public LocalDateTime getUsageDate() {
        return super.getProcessDate(); 
    }
}
