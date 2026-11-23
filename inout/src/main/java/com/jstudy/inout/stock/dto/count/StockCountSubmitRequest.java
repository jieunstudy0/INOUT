package com.jstudy.inout.stock.dto.count;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 직원이 확인한 실재고 제출. countedQty가 비어 있는 줄은 이번에 세지 않은 것으로 본다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockCountSubmitRequest {

    @NotEmpty(message = "제출할 품목이 없습니다.")
    @Valid
    private List<Line> lines;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {

        @NotNull(message = "줄 ID는 필수입니다.")
        private Long lineId;

        @Min(value = 0, message = "실재고는 0개 이상이어야 합니다.")
        @Max(value = 99_999, message = "실재고는 99,999개 이하로 입력해 주세요.")
        private Integer countedQty;
    }
}
