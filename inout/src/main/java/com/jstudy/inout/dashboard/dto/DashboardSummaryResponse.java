package com.jstudy.inout.dashboard.dto;

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
    
    private long lowStockCount;  
    
    private long pendingOrderCount;  
    
    private long unreadInquiryCount; 

    private int todayInCount;   
    
    private int todayOutCount;     

}
