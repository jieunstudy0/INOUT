package com.jstudy.inout.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderDetailStatus {

    WAITING("대기"), 
    APPROVED("발주 승인"), 
    DELAYED("발주 지연"), 
    REJECTED("발주 반려");

    private final String description;

    public boolean isProcessed() {
        return this == APPROVED || this == REJECTED;
    }

    public boolean needsStockDeduction() {
        return this == APPROVED;
    }
    
    public boolean isWaiting() {
        return this == WAITING;
    }
    
    public boolean canUpdate() {
        return this == WAITING || this == DELAYED; 
    }
}
