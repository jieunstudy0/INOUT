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
public class DashboardSummaryResponse {

    private String userName;
    private String storeName;

    private long todayNewOrderCount;    
    private long lowStockCount;     
    private long todayOrderAmount;     

    private long pendingDeliveryCount;   
    private long shippingDeliveryCount;  
    private long completedDeliveryCount; 

    private long normalStockCount;       
    private long outOfStockCount;   
    private long totalActiveStockCount; 


    private long pendingOrderCount;    
    private long completedOrderCount;    
    private long rejectedOrderCount;    
    private long totalOrderCount;        

    private int todayInCount;          
    private int todayOutCount;         

    private long unreadInquiryCount;
    private long waitingCsInquiryCount;
    private long aiDraftCompletedCount;
    private long aiSuggestedPendingOrderCount;

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