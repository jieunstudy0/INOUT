package com.jstudy.inout.payment.dto;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        @NotNull(message = "충전 대상 사용자 ID는 필수입니다.")
        private Long targetUserId;  
        @NotNull(message = "충전 금액은 필수입니다.")
        @Positive(message = "충전 금액은 0보다 커야 합니다.")
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