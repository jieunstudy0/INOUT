package com.jstudy.inout.payment.dto;

import com.jstudy.inout.payment.entity.TransactionType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;

public class DepositEmpDto {

    @Getter
    @Builder
    public static class HistoryResponse {
        private Long currentBalance; 
        private Page<HistoryItem> histories; 
    }

    @Getter
    @Builder
    public static class HistoryItem {
        private Long id;
        private TransactionType type; 
        private Long amount;
        private String description;
        private LocalDateTime createdAt;
    }
}