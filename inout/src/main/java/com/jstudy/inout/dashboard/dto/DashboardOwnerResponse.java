package com.jstudy.inout.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardOwnerResponse {

    private String ownerName;
    private String storeName;
    private Long storeId;
    private long depositBalance;
    private long todayOrderCount;
    private long pendingOrderCount;
    private long readyDeliveryCount;
    private long shippingDeliveryCount;
    private long completedDeliveryCount;
    private long pendingLeaveCount;
    private long staffCount;
    private long lockedStaffCount;
}
