package com.jstudy.inout.order.dto;

import java.time.LocalDateTime;
import java.util.List;
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
    private List<ItemDto> items;

    @Getter
    @Builder
    public static class ItemDto {
        private Long orderDetailId;
        private String itemName;
        private Integer quantity;
        private Long priceSnapshot;
        private Long subTotal;
        private OrderDetailStatus status;
        private boolean isAiSuggested;
        private String aiReason;
    }
}
