package com.jstudy.inout.stock.dto.count;

import com.jstudy.inout.stock.entity.StockCountLine;
import com.jstudy.inout.stock.entity.StockCountReason;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockCountLineResponse {

    private Long lineId;
    private Long storeStockId;
    private Integer lineNo;
    private Long itemId;
    private String itemName;
    private String location;
    private String unitDescription;
    /** 출력 시점 매장 재고 (영수증에 찍힌 값) */
    private Integer systemQty;
    private Integer minStockLevel;
    private StockCountReason reason;
    private Integer aiCountedQty;
    private Boolean aiConfident;
    private Integer countedQty;
    /** 실재고 - 출력 시점 시스템 재고 */
    private Integer diff;
    private Integer appliedDiff;
    /** 지금 매장 재고. 점주 확인 화면에서만 채운다. */
    private Integer currentStock;
    /** 승인하면 바뀔 재고 = 지금 재고 + diff. 점주 확인 화면에서만 채운다. */
    private Integer expectedAfterApply;

    public static StockCountLineResponse from(StockCountLine line, Integer currentStock) {
        Integer diff = line.getDiff();
        return StockCountLineResponse.builder()
                .lineId(line.getId())
                .storeStockId(line.getStoreStock().getId())
                .lineNo(line.getLineNo())
                .itemId(line.getItemId())
                .itemName(line.getItemName())
                .location(line.getLocation())
                .unitDescription(line.getUnitDescription())
                .systemQty(line.getSystemQty())
                .minStockLevel(line.getMinStockLevel())
                .reason(line.getReason())
                .aiCountedQty(line.getAiCountedQty())
                .aiConfident(line.getAiConfident())
                .countedQty(line.getCountedQty())
                .diff(diff)
                .appliedDiff(line.getAppliedDiff())
                .currentStock(currentStock)
                .expectedAfterApply(currentStock != null && diff != null ? currentStock + diff : null)
                .build();
    }
}
