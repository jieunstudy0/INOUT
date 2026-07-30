package com.jstudy.inout.dashboard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardEmpResponse {

    private String userName;
    private String storeName;
    private long depositBalance;
    /** 서버 기준 잔여 연차 일수 (기본 부여 − 승인 사용분) */
    private int remainingLeaveDays;
    private int cartItemCount;

    private int inProgressOrderCount;
    private int totalOrderCount;
    private int completedOrderCount;
    private int rejectedOrderCount;

    private int todayStockUseCount;
    private long totalActiveStockCount;
    private int normalStockCount;
    private int lowStockCount;
    private int outOfStockCount;

    private List<ActivityItem> recentActivities;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActivityItem {
        private String type;     
        private String message;
        private String time;     
        private String severity; 
    }
}