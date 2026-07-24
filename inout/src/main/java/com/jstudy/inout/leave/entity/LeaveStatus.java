package com.jstudy.inout.leave.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveStatus {

    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    HOLD("보류");

    private final String description;

    public boolean isProcessable() {
        return this == PENDING || this == HOLD;
    }
}
