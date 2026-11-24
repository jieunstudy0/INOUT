package com.jstudy.inout.stock.entity;

/** 매장 재고가 바뀐 이유 */
public enum StoreStockChangeType {
    /** 본사 배송 수령 확인 (+) */
    RECEIVE("수령"),
    /** 직원 사용 처리 (-) */
    USE("사용"),
    /** 점주 조정 — 폐기·파손·매장 자체 구매·처음 등록 (+/-) */
    ADJUST("조정"),
    /** 실사표 점주 승인 (+/-) */
    COUNT("실사");

    private final String label;

    StoreStockChangeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
