package com.jstudy.inout.stock.dto.store;

import java.time.LocalDateTime;
import java.util.List;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.entity.StoreStockHistory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 매장 재고 요청·응답 DTO 모음 */
public final class StoreStockDtos {

    private StoreStockDtos() {
    }

    @Getter
    @Builder
    public static class StockResponse {
        private Long storeStockId;
        private Long itemId;
        private String itemName;
        private String categoryName;
        private String unitDescription;
        private Integer quantity;
        private Integer minStockLevel;
        private String location;
        private boolean low;
        private LocalDateTime updatedAt;

        public static StockResponse from(StoreStock s) {
            Item item = s.getItem();
            return StockResponse.builder()
                    .storeStockId(s.getId())
                    .itemId(item.getItemId())
                    .itemName(item.getName())
                    .categoryName(item.getCategory() != null ? item.getCategory().getCategoryName() : "미지정")
                    .unitDescription(item.getUnitDescription())
                    .quantity(s.getQuantity())
                    .minStockLevel(s.getMinStockLevel())
                    .location(s.getLocation())
                    .low(s.isLow())
                    .updatedAt(s.getUpdatedAt())
                    .build();
        }
    }

    /** 점주가 매장 재고에 새로 등록할 수 있는 본사 상품 */
    @Getter
    @Builder
    public static class CatalogItem {
        private Long itemId;
        private String itemName;
        private String categoryName;
        private String unitDescription;
        private boolean registered;
    }

    @Getter
    @Builder
    public static class HistoryResponse {
        private Long historyId;
        private String type;
        private Integer changeQty;
        private Integer resultQty;
        private String memo;
        private String userName;
        private LocalDateTime createdAt;

        public static HistoryResponse from(StoreStockHistory h) {
            return HistoryResponse.builder()
                    .historyId(h.getId())
                    .type(h.getType().getLabel())
                    .changeQty(h.getChangeQty())
                    .resultQty(h.getResultQty())
                    .memo(h.getMemo())
                    .userName(h.getUser().getName())
                    .createdAt(h.getCreatedAt())
                    .build();
        }
    }

    /** 직원 사용 처리 */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UseRequest {
        @NotNull(message = "상품 ID는 필수입니다.")
        private Long itemId;

        @NotNull(message = "사용 수량은 필수입니다.")
        @Min(value = 1, message = "사용 수량은 1개 이상이어야 합니다.")
        @Max(value = 99_999, message = "사용 수량은 99,999개 이하로 입력해 주세요.")
        private Integer quantity;

        @Size(max = 100, message = "메모는 100자 이하로 입력해 주세요.")
        private String memo;
    }

    /** 점주 조정: 실제 수량으로 맞춘다. 매장에 없던 상품이면 새로 등록한다. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustRequest {
        @NotNull(message = "상품 ID는 필수입니다.")
        private Long itemId;

        @NotNull(message = "실제 수량은 필수입니다.")
        @Min(value = 0, message = "실제 수량은 0개 이상이어야 합니다.")
        @Max(value = 99_999, message = "실제 수량은 99,999개 이하로 입력해 주세요.")
        private Integer actualQuantity;

        @NotBlank(message = "조정 사유를 입력해 주세요.")
        @Size(max = 100, message = "조정 사유는 100자 이하로 입력해 주세요.")
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsRequest {
        @NotNull(message = "안전재고는 필수입니다.")
        @Min(value = 0, message = "안전재고는 0개 이상이어야 합니다.")
        @Max(value = 99_999, message = "안전재고는 99,999개 이하로 입력해 주세요.")
        private Integer minStockLevel;

        @Size(max = 20, message = "보관 위치는 20자 이하로 입력해 주세요.")
        private String location;
    }

    // ── 배송 수령 확인 ─────────────────────────────────────────────

    @Getter
    @Builder
    public static class PendingReceipt {
        private Long orderId;
        private String deliveryStatus;
        private String trackingNumber;
        private String carrier;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
        private String requestUserName;
        private List<PendingReceiptLine> lines;
    }

    @Getter
    @Builder
    public static class PendingReceiptLine {
        private Long orderDetailId;
        private Long itemId;
        private String itemName;
        private String unitDescription;
        private Integer orderedQty;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveRequest {
        @NotEmpty(message = "수령 품목이 없습니다.")
        @Valid
        private List<ReceiveLine> lines;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveLine {
        @NotNull(message = "발주 상세 ID는 필수입니다.")
        private Long orderDetailId;

        @NotNull(message = "수령 수량은 필수입니다.")
        @Min(value = 0, message = "수령 수량은 0개 이상이어야 합니다.")
        private Integer receivedQty;
    }

    @Getter
    @AllArgsConstructor
    public static class ReceiveResponse {
        private int itemCount;
        /** 발주보다 덜 받은 품목 수 */
        private int shortItemCount;
    }
}
