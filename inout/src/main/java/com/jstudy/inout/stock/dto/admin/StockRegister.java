package com.jstudy.inout.stock.dto.admin;

import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockRegister {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Integer categoryId;

    @NotNull(message = "단가는 필수입니다.")
    @Min(value = 0, message = "단가는 0원 이상이어야 합니다.")
    private Long unitPrice;

    @Min(value = 0, message = "최소 재고 수량은 0개 이상이어야 합니다.")
    private Integer minStockLevel;

    private String unitDescription;
    
    private String description;
    
    public Item toEntity(ItemCategory category) {
        return Item.builder()
                .name(this.name)
                .category(category)
                .unitPrice(this.unitPrice)
                .minStockLevel(this.minStockLevel != null ? this.minStockLevel : 0)
                .currentStock(0)
                .unitDescription(this.unitDescription)
                .description(this.description)
                .build();
    }
}
