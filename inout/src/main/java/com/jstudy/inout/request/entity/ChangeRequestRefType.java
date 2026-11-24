package com.jstudy.inout.request.entity;

/** 요청이 가리키는 대상 (어느 발주·매장 재고·연차에 대한 요청인지) */
public enum ChangeRequestRefType {
    NONE,
    ORDER,
    STORE_STOCK,
    LEAVE
}
