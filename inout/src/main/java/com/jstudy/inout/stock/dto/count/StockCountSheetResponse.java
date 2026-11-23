package com.jstudy.inout.stock.dto.count;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.stock.entity.StockCountSheet;
import com.jstudy.inout.stock.entity.StockCountStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockCountSheetResponse {

    private Long sheetId;
    private String sheetCode;
    private String storeName;
    private StockCountStatus status;
    private LocalDateTime createdAt;
    private String createdByName;
    private String photoUrl;
    private Boolean sheetCodeMatched;
    private LocalDateTime scannedAt;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private List<StockCountLineResponse> lines;

    /**
     * @param currentQtyByStoreStockId 점주 확인 화면이면 매장 재고 행별 지금 수량, 아니면 빈 맵
     */
    public static StockCountSheetResponse from(StockCountSheet sheet, Map<Long, Integer> currentQtyByStoreStockId) {
        return StockCountSheetResponse.builder()
                .sheetId(sheet.getId())
                .sheetCode(sheet.getSheetCode())
                .storeName(sheet.getStore().getName())
                .status(sheet.getStatus())
                .createdAt(sheet.getCreatedAt())
                .createdByName(nameOf(sheet.getCreatedBy(), "자동 생성"))
                .photoUrl(sheet.getPhotoUrl())
                .sheetCodeMatched(sheet.getSheetCodeMatched())
                .scannedAt(sheet.getScannedAt())
                .submittedByName(nameOf(sheet.getSubmittedBy(), null))
                .submittedAt(sheet.getSubmittedAt())
                .reviewedByName(nameOf(sheet.getReviewedBy(), null))
                .reviewedAt(sheet.getReviewedAt())
                .rejectReason(sheet.getRejectReason())
                .lines(sheet.getLines().stream()
                        .map(line -> StockCountLineResponse.from(line,
                                currentQtyByStoreStockId.get(line.getStoreStock().getId())))
                        .toList())
                .build();
    }

    static String nameOf(User user, String fallback) {
        return user != null ? user.getName() : fallback;
    }
}
