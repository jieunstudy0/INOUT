package com.jstudy.inout.stock.dto.emp;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockUseRequest {
	
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long itemId;
    
    @Min(value = 1, message = "사용 수량은 1개 이상이어야 합니다.")
    private int quantity;
    
    private String memo;
}
