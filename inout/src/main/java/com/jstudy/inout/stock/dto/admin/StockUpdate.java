package com.jstudy.inout.stock.dto.admin;

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
public class StockUpdate {
	
    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Integer categoryId;

    @NotNull(message = "단가는 필수입니다.")
    @Min(value = 0)
    private Long unitPrice;

    @Min(value = 0)
    private Integer minStockLevel;

    private String unitDescription;
    
    private String description;
    
}
