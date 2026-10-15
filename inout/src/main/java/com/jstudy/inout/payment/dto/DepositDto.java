package com.jstudy.inout.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

public class DepositDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChargeRequest {
        @NotNull(message = "충전 금액은 필수입니다.")
        @Positive(message = "충전 금액은 0보다 커야 합니다.")
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
        @NotNull(message = "환불 금액은 필수입니다.")
        @Positive(message = "환불 금액은 0보다 커야 합니다.")
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