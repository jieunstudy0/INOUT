package com.jstudy.inout.order.event;

import java.util.List;

public record OrderApprovedEvent(
        Long orderId,
        Long userId,
        Long storeId,
        List<ApprovedItem> items) {

    public record ApprovedItem(
            Long itemId,
            String itemName,
            int quantity,
            long priceSnapshot) {}
}
