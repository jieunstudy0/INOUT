package com.jstudy.inout.stock.entity;

import java.time.LocalDateTime;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import com.jstudy.inout.stock.exception.NotEnoughStockException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "item")
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel = 0;

    @Column(name = "unit_description", length = 100)
    private String unitDescription;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다. quantity=" + quantity);
        }
        this.currentStock += quantity;
    }


    public void removeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("출고 수량은 1 이상이어야 합니다. quantity=" + quantity);
        }
        int restStock = this.currentStock - quantity;
        if (restStock < 0) {
            throw NotEnoughStockException.withCurrentStock(this.currentStock, quantity);
        }
        this.currentStock = restStock;
    }


    public void updateInfo(String name, ItemCategory category, Long unitPrice,
            Integer minStockLevel, String unitDescription, String description) {
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.minStockLevel = minStockLevel;
        this.unitDescription = unitDescription;
        this.description = description;
    }

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}