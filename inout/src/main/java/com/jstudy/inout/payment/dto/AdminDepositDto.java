package com.jstudy.inout.payment.dto;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdminDepositDto {

    @Getter
    @Builder
    public static class FranchiseeInfo {
        private Long userId;
        private String userName;
        private String email; 
        private String storeName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminChargeRequest {
        private Long targetUserId;  
        private Long amount;        
        private String description; 
    }

    @Getter
    @Builder
    public static class ListResponse {
        private Summary summary;
        private Page<HistoryItem> histories;
    }

    @Getter
    @Builder
    public static class Summary {
        private long totalBalance;
        private long monthlyCharge;
        private long monthlyUsage;
    }

    @Getter
    @Builder
    public static class HistoryItem {
        private Long id;
        private String storeName;
        private String type;
        private long amount;
        private long balanceAfter;
        private String description;
        private LocalDateTime createdAt;
    }
}