package com.jstudy.inout.request.entity;

/**
 * 수정 요청 종류와, 그 종류를 처리하는 쪽.
 * 처리하는 쪽은 서버가 종류로 정한다 — 화면이 보낸 값으로 정하면 다른 곳으로 요청을 보낼 수 있기 때문이다.
 */
public enum ChangeRequestCategory {
    /** 배송 이상: 덜 옴·파손·다른 상품 (사진 첨부 권장) */
    DELIVERY_ISSUE("배송 이상", Handler.HQ),
    /** 본사 상품 정보 오류: 이름·단위·설명 */
    ITEM_INFO("상품 정보 오류", Handler.HQ),
    /** 매장 재고 수량 정정 (실사 날이 아닌데 차이를 발견) */
    STOCK_CORRECTION("매장 재고 정정", Handler.OWNER),
    /** 발주 수정·취소 (점주 결제 후라 직원이 직접 못 고칠 때) */
    ORDER_CHANGE("발주 수정·취소", Handler.OWNER),
    /** 승인된 연차 변경·취소 */
    LEAVE_CHANGE("연차 변경·취소", Handler.OWNER),
    /** 내 정보·근무 정보 */
    PROFILE_CHANGE("내 정보 수정", Handler.OWNER),
    /** 그 밖의 매장 요청 */
    OTHER("기타", Handler.OWNER);

    private final String label;
    private final Handler handler;

    ChangeRequestCategory(String label, Handler handler) {
        this.label = label;
        this.handler = handler;
    }

    public String getLabel() {
        return label;
    }

    public Handler getHandler() {
        return handler;
    }

    /** 처리하는 쪽: 소속 매장 점주 / 본사 */
    public enum Handler {
        OWNER, HQ
    }
}
