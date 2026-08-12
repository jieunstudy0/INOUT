package com.jstudy.inout.delivery.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DeliveryTrackingDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingResponse {
        private String carrier;
        private String trackingNumber;
        private String currentStatus;
        private boolean mockFallback;
        private List<TrackingEvent> events;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingEvent {
        private LocalDateTime time;
        private String location;
        private String status;
        private String description;
    }
}
