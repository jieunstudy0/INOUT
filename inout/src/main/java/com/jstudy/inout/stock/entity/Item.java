package com.jstudy.inout.stock.entity;

import java.time.LocalDateTime;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import com.jstudy.inout.stock.exception.NotEnoughStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false; 

    private LocalDateTime deletedAt;

   
    public void addStock(int quantity) {
        this.currentStock += quantity;
    }

    public void removeStock(int quantity) {
        int restStock = this.currentStock - quantity;
        if (restStock < 0) {
        	throw NotEnoughStockException.withCurrentStock(this.currentStock, quantity);
        }
        this.currentStock = restStock;
    }
    
    public void updateInfo(String name, ItemCategory category, Long unitPrice, 
            Integer minStockLevel, String unitDescription, String description 
            ) 
    { 
    	
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
