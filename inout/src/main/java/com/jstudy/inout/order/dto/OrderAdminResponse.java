package com.jstudy.inout.order.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.jstudy.inout.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAdminResponse {
    private Long orderRequestId;
    private String storeName;
    private String employeeName;
    private LocalDateTime requestDate;
    private OrderStatus status;
    private Long totalPrice;
    private String representativeItemName;
    private Integer itemCount;
    /** true = AI 자동 발주로 생성된 초안 (OrderDetail 중 isAiSuggested=true 존재) */
    private boolean aiSuggested;
    /**
     * AI 발주 제안 건의 품목별 추천 근거 목록.
     * aiSuggested=true 인 경우에만 populated 되며, 프론트엔드 발주 목록에 근거 콜아웃을 렌더링하는 데 사용됩니다.
     */
    private List<AiReasonItem> aiReasonItems;

    /** 품목별 AI 추천 근거 */
    public record AiReasonItem(String itemName, String reason) {}
}
