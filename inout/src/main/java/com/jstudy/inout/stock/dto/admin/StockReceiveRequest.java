package com.jstudy.inout.stock.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StockReceiveRequest {
	
    @NotNull(message = "상품 ID는 필수입니다.")
    private final Long itemId;

    @Min(value = 1, message = "입고 수량은 1개 이상이어야 합니다.")
    private final int quantity;

    private final String memo;
}
