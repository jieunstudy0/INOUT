package com.jstudy.inout.leave.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeaveType {

    ANNUAL("연차"),
    HALF_DAY("반차"),
    SICK("병가");

    private final String description;
}
