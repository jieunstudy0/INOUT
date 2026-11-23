package com.jstudy.inout.stock.dto.count;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 승인 결과: 재고가 늘어난 상품 수, 줄어든 상품 수, 차이가 없던 상품 수. */
@Getter
@AllArgsConstructor
public class StockCountApplyResponse {
    private int increasedCount;
    private int decreasedCount;
    private int unchangedCount;
}
