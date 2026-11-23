package com.jstudy.inout.request.entity;

/** REQUESTED(접수) → IN_PROGRESS(처리 중) → DONE(완료) / REJECTED(반려). DONE·REJECTED는 끝난 상태다. */
public enum ChangeRequestStatus {
    REQUESTED("접수"),
    IN_PROGRESS("처리 중"),
    DONE("완료"),
    REJECTED("반려");

    private final String label;

    ChangeRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFinished() {
        return this == DONE || this == REJECTED;
    }
}
