package com.jstudy.inout.inquiry.entity;

public enum InquiryTargetType {
    ADMIN("본사"),
    OWNER("점주");

    private final String description;

    InquiryTargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
