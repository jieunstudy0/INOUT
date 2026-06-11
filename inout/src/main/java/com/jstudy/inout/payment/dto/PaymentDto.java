package com.jstudy.inout.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PaymentDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long orderId;    
        private Long amount;    
    }

    @Getter
    @Builder
    public static class Response {
        private Long orderId;
        private Long paidAmount;
        private Long remainingBalance;
        private String message;
    }
}