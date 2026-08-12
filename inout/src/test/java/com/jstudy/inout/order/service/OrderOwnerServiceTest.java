package com.jstudy.inout.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.OwnerOrderModifyRequest;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.service.DepositService;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderOwnerServiceTest {

    @InjectMocks
    private OrderOwnerService orderOwnerService;

    @Mock private OrderRequestRepository orderRequestRepository;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private DepositService depositService;

    @Test
    @DisplayName("점주 수정·결제 승인 - 품목 삭제·수량 축소 후 재계산 금액만큼 예치금 차감하고 ORDERED로 전환한다")
    void modifyAndApprove_recalculatesTotalAndDeductsDeposit() {
        Store store = Store.builder().id(10L).name("1호점").address("서울").build();
        User owner = User.builder().id(5L).name("점주").store(store).build();
        User emp = User.builder().id(7L).name("직원").store(store).dailyDepositLimit(null).todayUsedDeposit(0L).build();

        Item keep = Item.builder().itemId(1L).name("생수").unitPrice(1000L).build();
        Item drop = Item.builder().itemId(2L).name("컵").unitPrice(500L).build();

        OrderDetail d1 = OrderDetail.builder()
                .orderDetailId(11L).item(keep).requestQuantity(10)
                .itemPriceSnapshot(1000L).status(OrderDetailStatus.WAITING).build();
        OrderDetail d2 = OrderDetail.builder()
                .orderDetailId(12L).item(drop).requestQuantity(4)
                .itemPriceSnapshot(500L).status(OrderDetailStatus.WAITING).build();

        OrderRequest order = OrderRequest.builder()
                .id(100L)
                .requestUser(emp)
                .status(OrderStatus.REQUESTED)
                .totalPrice(12_000L)
                .orderDetails(new ArrayList<>(List.of(d1, d2)))
                .build();
        d1 = OrderDetail.builder().orderDetailId(11L).item(keep).requestQuantity(10)
                .itemPriceSnapshot(1000L).status(OrderDetailStatus.WAITING).orderRequest(order).build();
        d2 = OrderDetail.builder().orderDetailId(12L).item(drop).requestQuantity(4)
                .itemPriceSnapshot(500L).status(OrderDetailStatus.WAITING).orderRequest(order).build();
        order.getOrderDetails().clear();
        order.getOrderDetails().add(d1);
        order.getOrderDetails().add(d2);

        given(userRepository.findById(5L)).willReturn(Optional.of(owner));
        given(orderRequestRepository.findByIdForUpdateWithDetails(100L)).willReturn(Optional.of(order));
        given(userRepository.findByIdForUpdate(7L)).willReturn(Optional.of(emp));

        OwnerOrderModifyRequest request = OwnerOrderModifyRequest.builder()
                .items(List.of(OwnerOrderModifyRequest.ItemLine.builder().itemId(1L).quantity(3).build()))
                .build();

        Long resultId = orderOwnerService.modifyAndApprove(5L, 100L, request);

        assertThat(resultId).isEqualTo(100L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(order.getTotalPrice()).isEqualTo(3_000L);
        assertThat(order.getOrderDetails()).hasSize(1);
        assertThat(order.getOrderDetails().get(0).getRequestQuantity()).isEqualTo(3);

        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        verify(depositService).deductDeposit(eq(7L), eq(5L), amountCaptor.capture(), any(), eq(100L));
        assertThat(amountCaptor.getValue()).isEqualTo(3_000L);
    }

    @Test
    @DisplayName("점주 수정·결제 승인 실패 - REQUESTED가 아니면 거부한다")
    void modifyAndApprove_rejectsNonRequested() {
        Store store = Store.builder().id(10L).name("1호점").build();
        User owner = User.builder().id(5L).name("점주").store(store).build();
        User emp = User.builder().id(7L).name("직원").store(store).build();
        OrderRequest order = OrderRequest.builder()
                .id(100L).requestUser(emp).status(OrderStatus.ORDERED).totalPrice(1000L)
                .orderDetails(new ArrayList<>()).build();

        given(userRepository.findById(5L)).willReturn(Optional.of(owner));
        given(orderRequestRepository.findByIdForUpdateWithDetails(100L)).willReturn(Optional.of(order));

        OwnerOrderModifyRequest request = OwnerOrderModifyRequest.builder()
                .items(List.of(OwnerOrderModifyRequest.ItemLine.builder().itemId(1L).quantity(1).build()))
                .build();

        assertThatThrownBy(() -> orderOwnerService.modifyAndApprove(5L, 100L, request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("직원 기안");
    }
}
