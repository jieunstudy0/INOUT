package com.jstudy.inout.leave.dto;

import com.jstudy.inout.leave.entity.AnnualLeave;
import com.jstudy.inout.leave.entity.LeaveStatus;
import com.jstudy.inout.leave.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AnnualLeaveDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotNull(message = "시작일은 필수입니다.")
        private LocalDate startDate;

        @NotNull(message = "종료일은 필수입니다.")
        private LocalDate endDate;

        @NotNull(message = "연차 종류를 선택해주세요.")
        private LeaveType type;

        @Size(max = 500, message = "사유는 500자 이내로 입력해주세요.")
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessRequest {
        @NotNull(message = "처리 상태를 선택해주세요.")
        private LeaveStatus status;

        @Size(max = 500, message = "반려 사유는 500자 이내로 입력해주세요.")
        private String rejectReason;
    }

    @Getter
    @Builder
    public static class ListItem {
        private Long leaveId;
        private String employeeName;
        private LocalDate startDate;
        private LocalDate endDate;
        private LeaveType type;
        private LeaveStatus status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long leaveId;
        private Long userId;
        private String employeeName;
        private LocalDate startDate;
        private LocalDate endDate;
        private LeaveType type;
        private String reason;
        private LeaveStatus status;
        private String rejectReason;
        private String processorName;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    public static ListItem toListItem(AnnualLeave leave) {
        return ListItem.builder()
                .leaveId(leave.getId())
                .employeeName(leave.getUser().getName())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .type(leave.getType())
                .status(leave.getStatus())
                .createdAt(leave.getCreatedAt())
                .build();
    }

    public static DetailResponse toDetail(AnnualLeave leave) {
        return DetailResponse.builder()
                .leaveId(leave.getId())
                .userId(leave.getUser().getId())
                .employeeName(leave.getUser().getName())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .type(leave.getType())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .rejectReason(leave.getRejectReason())
                .processorName(leave.getProcessor() != null ? leave.getProcessor().getName() : null)
                .processedAt(leave.getProcessedAt())
                .createdAt(leave.getCreatedAt())
                .updatedAt(leave.getUpdatedAt())
                .build();
    }
}
