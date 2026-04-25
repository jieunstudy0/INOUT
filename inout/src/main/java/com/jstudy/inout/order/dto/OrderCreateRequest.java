package com.jstudy.inout.order.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
	
    @NotEmpty(message = "발주할 상품을 선택해주세요.")
    private List<Long> cartDetailIds; 
    
    private String memo; 

}
