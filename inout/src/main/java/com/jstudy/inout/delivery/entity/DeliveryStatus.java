package com.jstudy.inout.delivery.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryStatus {

    READY("배송 준비"),
    SHIPPING("배송 중"),
    COMPLETED("배송 완료");

    private final String description;
}
