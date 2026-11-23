package com.jstudy.inout.stock.dto.count;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockCountRejectRequest {

    @NotBlank(message = "반려 사유를 입력해 주세요.")
    @Size(max = 255, message = "반려 사유는 255자 이하로 입력해 주세요.")
    private String reason;
}
