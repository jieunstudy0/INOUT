package com.jstudy.inout.order.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderStatusEnumTest {

    @Test
    @DisplayName("OrderDetailStatus - 승인(APPROVED) 상태만 재고 차감이 필요하다")
    void orderDetailStatus_NeedsStockDeduction() {
        assertThat(OrderDetailStatus.APPROVED.needsStockDeduction()).isTrue();

        assertThat(OrderDetailStatus.WAITING.needsStockDeduction()).isFalse();
        assertThat(OrderDetailStatus.DELAYED.needsStockDeduction()).isFalse();
        assertThat(OrderDetailStatus.REJECTED.needsStockDeduction()).isFalse();
    }

    @Test
    @DisplayName("OrderDetailStatus - 승인(APPROVED) 또는 반려(REJECTED)는 처리 완료(isProcessed)로 간주한다")
    void orderDetailStatus_IsProcessed() {
        assertThat(OrderDetailStatus.APPROVED.isProcessed()).isTrue();
        assertThat(OrderDetailStatus.REJECTED.isProcessed()).isTrue();

        assertThat(OrderDetailStatus.WAITING.isProcessed()).isFalse();
        assertThat(OrderDetailStatus.DELAYED.isProcessed()).isFalse();
    }

    @Test
    @DisplayName("OrderStatus - 발주 요청(REQUESTED) 상태일 때만 사용자가 취소할 수 있다")
    void orderStatus_IsCancelable() {
        assertThat(OrderStatus.REQUESTED.isCancelable()).isTrue();

        assertThat(OrderStatus.ORDERED.isCancelable()).isFalse();
        assertThat(OrderStatus.PARTIAL.isCancelable()).isFalse();
        assertThat(OrderStatus.APPROVED.isCancelable()).isFalse();
        assertThat(OrderStatus.REJECTED.isCancelable()).isFalse();
        assertThat(OrderStatus.CANCELLED.isCancelable()).isFalse();
    }

    @Test
    @DisplayName("OrderStatus - 완료되거나 반려, 취소된 건은 종료(isFinished) 상태로 간주한다")
    void orderStatus_IsFinished() {
        assertThat(OrderStatus.APPROVED.isFinished()).isTrue();
        assertThat(OrderStatus.REJECTED.isFinished()).isTrue();
        assertThat(OrderStatus.CANCELLED.isFinished()).isTrue();

        assertThat(OrderStatus.REQUESTED.isFinished()).isFalse();
        assertThat(OrderStatus.ORDERED.isFinished()).isFalse();
        assertThat(OrderStatus.PARTIAL.isFinished()).isFalse();
    }
}
