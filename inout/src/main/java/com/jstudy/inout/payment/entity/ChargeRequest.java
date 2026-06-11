package com.jstudy.inout.payment.entity;

import com.jstudy.inout.common.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChargeRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User requestUser; 

    private Long amount;

    @Enumerated(EnumType.STRING)
    private ChargeStatus status; 

    private LocalDateTime requestDate;
    private LocalDateTime processDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private User processor; 

    private String rejectReason; 

    @Builder
    public ChargeRequest(User requestUser, Long amount) {
        this.requestUser = requestUser;
        this.amount = amount;
        this.status = ChargeStatus.PENDING;
        this.requestDate = LocalDateTime.now();
    }

    public void approve(User processor) {
        this.status = ChargeStatus.APPROVED;
        this.processor = processor;
        this.processDate = LocalDateTime.now();
    }

    public void reject(User processor, String reason) {
        this.status = ChargeStatus.REJECTED;
        this.processor = processor;
        this.rejectReason = reason;
        this.processDate = LocalDateTime.now();
    }
}