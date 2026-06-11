package com.jstudy.inout.delivery.dto;

import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DeliveryDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank
        @Size(max = 100)
        private String receiverName;

        @NotBlank
        @Size(max = 30)
        private String receiverPhone;

        @NotBlank
        @Size(max = 255)
        private String destinationAddress;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartShippingRequest {
        @NotBlank
        @Size(max = 100)
        private String trackingNumber;

        private LocalDateTime shippedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteDeliveryRequest {
        private LocalDateTime deliveredAt;
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long deliveryId;
        private Long orderId;
        private DeliveryStatus status;
        private String receiverName;
        private String receiverPhone;
        private String destinationAddress;
        private String trackingNumber;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Builder
    public static class ListItem {
        private Long deliveryId;
        private Long orderId;
        private String receiverName;
        private DeliveryStatus status;
        private String trackingNumber;
        private LocalDateTime createdAt;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
    }

    public static DetailResponse from(Delivery delivery) {
        return DetailResponse.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderRequest().getId())
                .status(delivery.getStatus())
                .receiverName(delivery.getReceiverName())
                .receiverPhone(delivery.getReceiverPhone())
                .destinationAddress(delivery.getDestinationAddress())
                .trackingNumber(delivery.getTrackingNumber())
                .shippedAt(delivery.getShippedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }

    public static ListItem toListItem(Delivery delivery) {
        return ListItem.builder()
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderRequest().getId())
                .receiverName(delivery.getReceiverName())
                .status(delivery.getStatus())
                .trackingNumber(delivery.getTrackingNumber())
                .createdAt(delivery.getCreatedAt())
                .shippedAt(delivery.getShippedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .build();
    }
}
