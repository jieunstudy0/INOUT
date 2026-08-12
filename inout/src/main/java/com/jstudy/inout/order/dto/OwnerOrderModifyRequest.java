package com.jstudy.inout.order.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerOrderModifyRequest {

    @NotEmpty(message = "수정할 품목 목록이 필요합니다.")
    @Valid
    private List<ItemLine> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemLine {
        @NotNull(message = "품목 ID가 필요합니다.")
        private Long itemId;

        @NotNull(message = "수량이 필요합니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        private Integer quantity;
    }
}
