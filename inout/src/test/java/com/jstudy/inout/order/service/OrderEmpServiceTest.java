package com.jstudy.inout.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.OrderCreateRequest;
import com.jstudy.inout.order.dto.OrderListResponse;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.CartDetailRepository;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;

@ExtendWith(MockitoExtension.class)
class OrderEmpServiceTest {

    @InjectMocks
    private OrderEmpService orderEmpService;

    @Mock private CartDetailRepository cartDetailRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRequestRepository orderRequestRepository;
    @Mock private OrderDetailRepository orderDetailRepository;

    @Test
    @DisplayName("발주 요청 성공 - 장바구니 항목이 주문으로 변환되고 장바구니에서 삭제된다")
    void submitOrderRequest_Success() {
        // given
        Long userId = 1L;
        OrderCreateRequest request = OrderCreateRequest.builder()
                .cartDetailIds(List.of(10L, 11L))
                .build();

        User user = User.builder().id(userId).build();
        
        Item item1 = Item.builder().itemId(100L).unitPrice(2000L).build();
        Item item2 = Item.builder().itemId(101L).unitPrice(3000L).build();
        
        CartDetail cart1 = CartDetail.builder().item(item1).quantity(2).build(); // 4000원
        CartDetail cart2 = CartDetail.builder().item(item2).quantity(1).build(); // 3000원 -> 총 7000원

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartDetailRepository.findAllById(request.getCartDetailIds())).willReturn(List.of(cart1, cart2));

        // when
        orderEmpService.submitOrderRequest(userId, request);

        // then
        // 1. 주문 마스터(OrderRequest)가 1번 저장됨
        verify(orderRequestRepository, times(1)).save(any(OrderRequest.class));
        
        // 2. 주문 상세(OrderDetail)가 장바구니 항목 개수(2번)만큼 저장됨
        verify(orderDetailRepository, times(2)).save(any(OrderDetail.class));
        
        // 3. 발주가 끝난 장바구니 항목들은 일괄 삭제 처리됨
        verify(cartDetailRepository, times(1)).updateDeletedStatusInBatch(anyList());
    }

    @Test
    @DisplayName("발주 조회 성공 - '상품명 외 N건' 형태로 대표 이름이 잘 만들어진다")
    void getMyOrderHistory_Success() {
        // given
        User user = User.builder().id(1L).build();
        Item item1 = Item.builder().name("키보드").build();
        Item item2 = Item.builder().name("마우스").build();
        Item item3 = Item.builder().name("모니터").build();

        OrderDetail d1 = OrderDetail.builder().item(item1).build();
        OrderDetail d2 = OrderDetail.builder().item(item2).build();
        OrderDetail d3 = OrderDetail.builder().item(item3).build();

        OrderRequest order = OrderRequest.builder()
                .id(100L)
                .requestUser(user)
                .orderDetails(List.of(d1, d2, d3)) // 총 3건
                .status(OrderStatus.REQUESTED)
                .build();

        given(orderRequestRepository.findAllByRequestUser_UserIdOrderByRequestDateDesc(1L))
                .willReturn(List.of(order));

        // when
        List<OrderListResponse> result = orderEmpService.getMyOrderHistory(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRepresentativeItemName()).isEqualTo("키보드 외 2건"); // 이름 변환 로직 검증
    }

    @Test
    @DisplayName("발주 취소 성공 - 상태가 취소로 변경되고 처리 시각이 기록된다")
    void cancelOrder_Success() {
        // given
        User user = User.builder().id(1L).build();
        
        OrderRequest order = OrderRequest.builder()
                .requestUser(user)
                .status(OrderStatus.REQUESTED) // 취소 가능한 상태
                .build();

        given(orderRequestRepository.findById(100L)).willReturn(Optional.of(order));

        // when
        orderEmpService.cancelOrder(1L, 100L);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getProcessDate()).isNotNull();
    }

    @Test
    @DisplayName("발주 취소 실패 - 이미 처리 중이거나 완료된 주문은 취소 불가")
    void cancelOrder_Fail_InvalidStatus() {
        // given
        User user = User.builder().id(1L).build();
        OrderRequest order = OrderRequest.builder()
                .requestUser(user)
                .status(OrderStatus.PARTIAL) // REQUESTED가 아닌 상태
                .build();

        given(orderRequestRepository.findById(100L)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderEmpService.cancelOrder(1L, 100L))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 처리 진행 중이거나 완료된 주문");
    }
}