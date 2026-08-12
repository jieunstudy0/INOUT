package com.jstudy.inout.payment.entity;

import lombok.Getter;

@Getter
public enum TransactionType {
    CHARGE("예치금 충전"),
    PAYMENT("상품 결제(차감)"),
    REFUND("결제 취소 및 환불");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }
}