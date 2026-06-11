package com.jstudy.inout.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class DashboardStatisticsResponse {
    
    private List<MonthlyTrend> monthlyTrends;      
    private List<StoreFrequency> storeFrequencies;  
    private List<ItemConsumption> topConsumedItems; 

    @Getter @Builder
    public static class MonthlyTrend {
        private String month;
        private Long totalAmount;
    }

    @Getter @Builder
    public static class StoreFrequency {
        private String storeName;
        private Long orderCount;
    }

    @Getter @Builder
    public static class ItemConsumption {
        private String itemName;
        private Long totalConsumed;
    }
}