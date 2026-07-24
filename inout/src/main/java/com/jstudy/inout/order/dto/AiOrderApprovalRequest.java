package com.jstudy.inout.order.dto;

import java.util.List;

public record AiOrderApprovalRequest(

    List<ItemDecision> items

) {

    public record ItemDecision(
        Long orderDetailId,
        boolean approve,
        Integer approvedQuantity
    ) {}
}
