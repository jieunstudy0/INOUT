package com.jstudy.inout.notification.entity;

/** 알림 종류. 화면에서 아이콘·분류에 쓴다. */
public enum NotificationType {
    // 발주·배송
    ORDER_REQUESTED,
    ORDER_APPROVED,
    ORDER_REJECTED,
    DELIVERY_SHIPPED,
    // 매장 재고 실사
    STOCK_COUNT_READY,
    STOCK_COUNT_SUBMITTED,
    STOCK_COUNT_APPLIED,
    STOCK_COUNT_REJECTED,
    // 연차
    LEAVE_REQUESTED,
    LEAVE_DECIDED,
    // 수정 요청
    CHANGE_REQUEST_CREATED,
    CHANGE_REQUEST_UPDATED
}
