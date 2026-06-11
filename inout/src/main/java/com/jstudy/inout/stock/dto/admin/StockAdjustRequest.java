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
public class StockAdjustRequest {

    @NotNull(message = "조정 수량은 필수입니다.")
    @Min(value = 0, message = "조정 수량은 0 이상이어야 합니다.")
    private Integer adjustedQuantity;

    @NotBlank(message = "조정 사유는 필수입니다.")
    private String reason;
}
