package com.jstudy.inout.order.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiOrderApprovalResponse {
    private int approvedCount;
    private int rejectedCount;
    private int failureCount;
    private List<FailedItem> failures;

    @Getter
    @Builder
    public static class FailedItem {
        private Long orderDetailId;
        private String reason;
    }
}
