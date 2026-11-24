package com.jstudy.inout.stock.entity;

/** 실사표에 오른 이유. 영수증에 짧은 표시로 찍힌다. */
public enum StockCountReason {
    /** 매장 재고 ≤ 매장 안전재고 (품절 포함) */
    LOW_STOCK,
    /** 최근 24시간 안에 매장에서 사용 처리한 기록이 있음 */
    RECENT_USE
}
