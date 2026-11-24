package com.jstudy.inout.request.dto;

import java.time.LocalDateTime;
import com.jstudy.inout.request.entity.ChangeRequest;
import com.jstudy.inout.request.entity.ChangeRequestCategory;
import com.jstudy.inout.request.entity.ChangeRequestRefType;
import com.jstudy.inout.request.entity.ChangeRequestStatus;
import com.jstudy.inout.request.entity.DeliveryIssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class ChangeRequestDtos {

    private ChangeRequestDtos() {
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotNull(message = "요청 종류를 선택해 주세요.")
        private ChangeRequestCategory category;

        /** 배송 이상의 세부 종류 (수량 부족·파손·오배송·기타). 다른 종류의 요청에서는 무시한다 */
        private DeliveryIssueType issueType;

        /** 대상이 없으면 NONE 또는 비움 */
        private ChangeRequestRefType refType;

        private Long refId;

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
        private String title;

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 1000, message = "내용은 1000자 이하로 입력해 주세요.")
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusRequest {
        @NotNull(message = "바꿀 상태를 선택해 주세요.")
        private ChangeRequestStatus status;

        @Size(max = 255, message = "답변은 255자 이하로 입력해 주세요.")
        private String message;
    }

    @Getter
    @Builder
    public static class Response {
        private Long requestId;
        private ChangeRequestCategory category;
        private String categoryLabel;
        /** 배송 이상의 세부 종류 (배송 이상이 아니면 null) */
        private DeliveryIssueType issueType;
        private String issueTypeLabel;
        /** OWNER: 소속 매장 점주가 처리 / HQ: 본사가 처리 */
        private ChangeRequestCategory.Handler handler;
        private ChangeRequestRefType refType;
        private Long refId;
        private String refLabel;
        private String title;
        private String content;
        private String photoUrl;
        private ChangeRequestStatus status;
        private String statusLabel;
        private String storeName;
        private String requesterName;
        private String handledByName;
        private LocalDateTime handledAt;
        private String responseMessage;
        private LocalDateTime createdAt;
        /** 지금 보는 사람이 이 요청의 상태를 바꿀 수 있는지 */
        private boolean canHandle;

        public static Response from(ChangeRequest r, boolean canHandle) {
            return Response.builder()
                    .requestId(r.getId())
                    .category(r.getCategory())
                    .categoryLabel(r.getCategory().getLabel())
                    .issueType(r.getIssueType())
                    .issueTypeLabel(r.getIssueType() != null ? r.getIssueType().getLabel() : null)
                    .handler(r.getHandler())
                    .refType(r.getRefType())
                    .refId(r.getRefId())
                    .refLabel(r.getRefLabel())
                    .title(r.getTitle())
                    .content(r.getContent())
                    .photoUrl(r.getPhotoUrl())
                    .status(r.getStatus())
                    .statusLabel(r.getStatus().getLabel())
                    .storeName(r.getStore().getName())
                    .requesterName(r.getRequester().getName())
                    .handledByName(r.getHandledBy() != null ? r.getHandledBy().getName() : null)
                    .handledAt(r.getHandledAt())
                    .responseMessage(r.getResponseMessage())
                    .createdAt(r.getCreatedAt())
                    .canHandle(canHandle && !r.getStatus().isFinished())
                    .build();
        }
    }
}
