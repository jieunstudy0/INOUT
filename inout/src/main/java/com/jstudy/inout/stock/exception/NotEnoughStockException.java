package com.jstudy.inout.stock.exception;

import com.jstudy.inout.common.exception.InoutException;

public class NotEnoughStockException extends InoutException {

    public NotEnoughStockException(String message) {
        super(message, 400, "NOT_ENOUGH_STOCK");
    }

    public static NotEnoughStockException withCurrentStock(int current, int request) {
        return new NotEnoughStockException(
            String.format("재고가 부족합니다. (현재: %d, 요청: %d)", current, request)
        );
    }
}
