package com.jstudy.inout.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    REQUESTED("발주 요청"), 
    PARTIAL("부분 처리/지연"), 
    COMPLETED("처리 완료"),
    REJECTED("주문 반려"),
    APPROVED("발주 승인"),
    CANCELLED("(사용자)직접 취소");

    private final String description; 

    public boolean isFinished() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED;
    }

    public boolean isCancelable() {
        return this == REQUESTED;
    }

    public boolean isPending() {
        return this == REQUESTED || this == PARTIAL;
    }
}