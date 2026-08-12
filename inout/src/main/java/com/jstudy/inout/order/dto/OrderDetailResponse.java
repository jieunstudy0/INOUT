package com.jstudy.inout.order.dto;

import com.jstudy.inout.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderDetailResponse {
    private Long orderRequestId;
    private LocalDateTime requestDate;
    private OrderStatus status;
    private String storeName;
    private String employeeName;
    private Long totalPrice;
    private String receiverName;
    private String receiverPhone;
    private String destinationAddress;
    private List<OrderDetailItemDto> items;

    @Getter
    @Builder
    public static class OrderDetailItemDto {
        private String itemName;
        private Integer quantity;
        private Long priceSnapshot;
        private Long subTotal;
    }
}