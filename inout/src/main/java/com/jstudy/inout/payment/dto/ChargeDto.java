package com.jstudy.inout.payment.dto;

import com.jstudy.inout.payment.entity.ChargeRequest;
import com.jstudy.inout.payment.entity.ChargeStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

public class ChargeDto {

    @Getter
    @NoArgsConstructor
    @lombok.Setter
    public static class Request {
        private Long amount; 
    }

    @Getter
    @NoArgsConstructor
    @lombok.Setter
    public static class RejectRequest {
        private String reason; 
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String requestUserName;
        private String storeName;
        private Long amount;
        private ChargeStatus status;
        private LocalDateTime requestDate;
        private LocalDateTime processDate;
        private String processorName;
        private String rejectReason;

        public static Response from(ChargeRequest entity) {
            String storeName = entity.getRequestUser().getStore() != null 
                    ? entity.getRequestUser().getStore().getName() : "미지정";
            String processorName = entity.getProcessor() != null 
                    ? entity.getProcessor().getName() : null;

            return Response.builder()
                    .id(entity.getId())
                    .requestUserName(entity.getRequestUser().getName())
                    .storeName(storeName)
                    .amount(entity.getAmount())
                    .status(entity.getStatus())
                    .requestDate(entity.getRequestDate())
                    .processDate(entity.getProcessDate())
                    .processorName(processorName)
                    .rejectReason(entity.getRejectReason())
                    .build();
        }
    }
}