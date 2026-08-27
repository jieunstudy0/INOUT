package com.jstudy.inout.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAdminDetailResponse {

    private Long orderRequestId;
    private LocalDateTime requestDate;
    private OrderStatus status;
    private String storeName;
    private String employeeName;
    private Long totalPrice;
    private String rejectReason;
    /** AI 본사 자동발주 여부 */
    private boolean aiSuggestedOrder;
    /** AI 발주 시 가상 공급처 표시명 */
    private String vendorName;
    /** AI 발주 시 예상 입고일시(승인일 + 3일) */
    private LocalDateTime expectedInboundAt;
    /** 화면 뱃지용 입고 상태 라벨 */
    private String inboundStatusLabel;
    private List<ItemDto> items;

    @Getter
    @Builder
    public static class ItemDto {
        private Long orderDetailId;
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private Long priceSnapshot;
        private Long subTotal;
        private OrderDetailStatus status;
        @JsonProperty("isAiSuggested")
        private boolean isAiSuggested;
        private String aiReason;
    }
}
