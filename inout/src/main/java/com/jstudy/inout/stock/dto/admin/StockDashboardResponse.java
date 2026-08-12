package com.jstudy.inout.stock.dto.admin;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockDashboardResponse {
    private List<StockAdminResponse> lowStock; 
    private List<StockAdminResponse> outOfStock; 
}
