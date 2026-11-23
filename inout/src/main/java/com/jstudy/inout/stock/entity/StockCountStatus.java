package com.jstudy.inout.stock.entity;

/**
 * 재고 실사표 진행 상태.
 * OPEN(출력·작성 중) → SUBMITTED(직원 제출, 점주 확인 대기) → APPLIED(점주 승인, 매장 재고 반영)
 * 점주가 반려하면 SUBMITTED → OPEN 으로 돌아가 직원이 다시 세고 제출한다.
 */
public enum StockCountStatus {
    OPEN,
    SUBMITTED,
    APPLIED
}
