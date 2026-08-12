package com.jstudy.inout.payment.entity;

public enum ChargeStatus {
    PENDING,    // 승인 대기중
    APPROVED,   // 승인 완료 (예치금 충전됨)
    REJECTED    // 반려됨
}