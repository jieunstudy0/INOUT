package com.jstudy.inout.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus { 
	
	REQUESTED("발주 요청(미결제)"), 
    PAID("결제 완료(승인 대기)"),   
    PARTIAL("발주 부분 승인"), 
    COMPLETED("발주 승인 완료"), 
    REJECTED("발주 반려"), 
    CANCELLED("발주 취소");

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