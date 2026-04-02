package com.jstudy.inout.order.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkOrderResponse {
    private int successCount; 
    private int failureCount; 
    private List<FailedOrder> failures; 

    @Getter
    @Builder
    public static class FailedOrder {
        private Long orderId;
        private String reason;
    }
}
