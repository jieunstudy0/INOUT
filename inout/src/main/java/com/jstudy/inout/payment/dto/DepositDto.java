package com.jstudy.inout.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

public class DepositDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChargeRequest {
        private Long amount;
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundRequest {
    	
    	@NotNull(message = "환불 대상 사용자 ID는 필수입니다.")
        private Long targetUserId;  
        private Long amount;
        private String description;
        private Long originalHistoryId; 
    }

    @Getter
    @Builder
    public static class Response {
        private Long userId;
        private Long currentBalance;
        private String message;
    }
}