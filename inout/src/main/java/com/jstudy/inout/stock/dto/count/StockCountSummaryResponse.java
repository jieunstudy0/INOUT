package com.jstudy.inout.stock.dto.count;

import java.time.LocalDateTime;
import com.jstudy.inout.stock.entity.StockCountLine;
import com.jstudy.inout.stock.entity.StockCountSheet;
import com.jstudy.inout.stock.entity.StockCountStatus;
import lombok.Builder;
import lombok.Getter;

/** 실사표 목록 한 줄. */
@Getter
@Builder
public class StockCountSummaryResponse {

    private Long sheetId;
    private String sheetCode;
    private StockCountStatus status;
    private LocalDateTime createdAt;
    private int lineCount;
    /** 실재고가 입력된 줄 수 */
    private int countedCount;
    /** 시스템 수량과 실재고가 다른 줄 수 */
    private int mismatchCount;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private String rejectReason;

    public static StockCountSummaryResponse from(StockCountSheet sheet) {
        return StockCountSummaryResponse.builder()
                .sheetId(sheet.getId())
                .sheetCode(sheet.getSheetCode())
                .status(sheet.getStatus())
                .createdAt(sheet.getCreatedAt())
                .lineCount(sheet.getLines().size())
                .countedCount((int) sheet.getLines().stream().filter(l -> l.getCountedQty() != null).count())
                .mismatchCount((int) sheet.getLines().stream()
                        .map(StockCountLine::getDiff)
                        .filter(d -> d != null && d != 0)
                        .count())
                .submittedByName(StockCountSheetResponse.nameOf(sheet.getSubmittedBy(), null))
                .submittedAt(sheet.getSubmittedAt())
                .rejectReason(sheet.getRejectReason())
                .build();
    }
}
