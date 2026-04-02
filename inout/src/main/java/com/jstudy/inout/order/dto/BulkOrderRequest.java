package com.jstudy.inout.order.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BulkOrderRequest {
    private List<Long> orderIds; 
}
