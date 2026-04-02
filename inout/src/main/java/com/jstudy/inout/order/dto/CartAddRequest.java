package com.jstudy.inout.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequest {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long itemId;

    @Min(value = 1, message = "주문 수량은 최소 1개 이상이어야 합니다.")
    private Integer quantity;
}
