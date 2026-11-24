package com.jstudy.inout.request.entity;

/**
 * 배송 이상 신고의 세부 종류. 배송 이상(DELIVERY_ISSUE) 요청에만 붙는다.
 * 본사가 받은 요청함에서 어떤 문제인지 바로 알아보고 처리 방법(추가 출고·교환·회수)을 고르게 한다.
 */
public enum DeliveryIssueType {
    /** 발주보다 덜 왔다 */
    SHORTAGE("수량 부족"),
    /** 상품이 깨지거나 상했다 */
    DAMAGED("파손"),
    /** 주문과 다른 상품이 왔다 */
    WRONG_ITEM("오배송"),
    /** 그 밖의 배송 문제 (포장 불량·유통기한·늦은 도착 등) */
    OTHER("기타");

    private final String label;

    DeliveryIssueType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
