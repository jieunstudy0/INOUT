package com.jstudy.inout.payment.dto;

import com.jstudy.inout.common.auth.util.UserDisplayNames;
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
            String processorName = entity.getProcessor() != null
                    ? entity.getProcessor().getName() : null;

            return Response.builder()
                    .id(entity.getId())
                    .requestUserName(UserDisplayNames.displayName(entity.getRequestUser()))
                    .storeName(UserDisplayNames.storeNameOr(entity.getRequestUser(), "미지정"))
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