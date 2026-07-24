package com.jstudy.inout.leave.entity;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "annual_leave")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnualLeave extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveType type;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status;

    @Column(length = 500)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor;

    private LocalDateTime processedAt;

    @Builder
    public AnnualLeave(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            LeaveType type,
            String reason) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reason = reason;
        this.status = LeaveStatus.PENDING;
    }

    public void approve(User processor) {
        this.status = LeaveStatus.APPROVED;
        this.processor = processor;
        this.processedAt = LocalDateTime.now();
        this.rejectReason = null;
    }

    public void reject(User processor, String rejectReason) {
        this.status = LeaveStatus.REJECTED;
        this.processor = processor;
        this.processedAt = LocalDateTime.now();
        this.rejectReason = rejectReason;
    }

    public void hold(User processor) {
        this.status = LeaveStatus.HOLD;
        this.processor = processor;
        this.processedAt = LocalDateTime.now();
        this.rejectReason = null;
    }
}
