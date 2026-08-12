package com.jstudy.inout.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프랜차이즈 3단계 발주 상태.
 * 직원 기안(REQUESTED) → 점주 결제(ORDERED) → 본사 승인(APPROVED) | 반려(REJECTED)
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    REQUESTED("직원 기안(승인 대기·미결제)"),
    ORDERED("점주 결제 완료(본사 승인 대기)"),
    APPROVED("본사 최종 승인(출고 준비)"),
    REJECTED("반려"),
    CANCELLED("발주 취소"),
    /** @deprecated 레거시 호환 — DB 마이그레이션 후 제거 예정 */
    @Deprecated
    PAID("결제 완료(레거시→ORDERED)"),
    /** @deprecated 레거시 호환 */
    @Deprecated
    PARTIAL("부분 승인(레거시)"),
    /** @deprecated 레거시 호환 — APPROVED로 매핑 */
    @Deprecated
    COMPLETED("승인 완료(레거시→APPROVED)");

    private final String description;

    public boolean isFinished() {
        return this == APPROVED || this == COMPLETED || this == REJECTED || this == CANCELLED;
    }

    public boolean isCancelable() {
        return this == REQUESTED;
    }

    public boolean isPending() {
        return this == REQUESTED || this == ORDERED || this == PAID || this == PARTIAL;
    }

    /** 점주 결제 완료 여부 (본사 대기) */
    public boolean isAwaitingHq() {
        return this == ORDERED || this == PAID;
    }

    /** 본사 승인 완료 (출고/배송 가능) */
    public boolean isHqApproved() {
        return this == APPROVED || this == COMPLETED;
    }
}
