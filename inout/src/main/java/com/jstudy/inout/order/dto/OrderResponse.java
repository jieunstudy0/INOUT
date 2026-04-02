package com.jstudy.inout.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {
 private Long id;
 private String storeName;
 private String employeeName;
 private LocalDateTime requestDate;
 private String status;
 private List<OrderDetailResponse> items; 

}
