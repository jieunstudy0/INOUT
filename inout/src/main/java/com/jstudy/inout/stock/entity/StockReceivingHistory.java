package com.jstudy.inout.stock.entity;

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
@Table(name = "stock_receiving_history")
@SuperBuilder
@NoArgsConstructor
@AttributeOverrides({
    @AttributeOverride(name = "processDate", column = @Column(name = "process_date")),
    @AttributeOverride(name = "user", column = @Column(name = "process_user_id"))
})
public class StockReceivingHistory extends BaseStockHistory {
    private Integer receivingQuantity;
}
