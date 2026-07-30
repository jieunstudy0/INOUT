package com.jstudy.inout.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.dto.OrderCreateRequest;
import com.jstudy.inout.order.dto.OrderListResponse;
import com.jstudy.inout.order.entity.Cart;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.CartDetailRepository;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;
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
class OrderEmpServiceTest {

    @InjectMocks
    private OrderEmpService orderEmpService;

    @Mock private CartDetailRepository cartDetailRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRequestRepository orderRequestRepository;
    @Mock private OrderDetailRepository orderDetailRepository;
    @Mock private ItemRepository itemRepository;

    @Test
    @DisplayName("발주 요청 성공 - 장바구니 항목이 주문으로 변환되고 장바구니에서 삭제된다")
    void submitOrderRequest_Success() {
        Long userId = 1L;
        OrderCreateRequest request = OrderCreateRequest.builder()
                .cartDetailIds(List.of(10L, 11L))
                .build();

        Store store = Store.builder().name("본점").address("서울시 종로구").build();
        User user = User.builder().id(userId).name("김직원").phone("010-2222-3333").store(store).build();
        Cart cart = Cart.builder().cartId(1L).user(user).build();

        Item item1 = Item.builder().itemId(100L).unitPrice(2000L).currentStock(50).build();
        Item item2 = Item.builder().itemId(101L).unitPrice(3000L).currentStock(50).build();

        CartDetail cart1 = CartDetail.builder().cart(cart).item(item1).quantity(2).build();
        CartDetail cart2 = CartDetail.builder().cart(cart).item(item2).quantity(1).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartDetailRepository.findWithCartAndUserByIds(request.getCartDetailIds())).willReturn(List.of(cart1, cart2));
        given(itemRepository.findByIdWithLock(100L)).willReturn(Optional.of(item1));
        given(itemRepository.findByIdWithLock(101L)).willReturn(Optional.of(item2));

        orderEmpService.submitOrderRequest(userId, request);

        ArgumentCaptor<OrderRequest> orderCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderRequestRepository, times(1)).save(orderCaptor.capture());
        verify(orderDetailRepository, times(2)).save(any(OrderDetail.class));
        verify(cartDetailRepository, times(1)).updateDeletedStatusInBatch(anyList());

        OrderRequest saved = orderCaptor.getValue();
        assertThat(saved.getReceiverName()).isEqualTo("김직원");
        assertThat(saved.getReceiverPhone()).isEqualTo("010-2222-3333");
        assertThat(saved.getDestinationAddress()).isEqualTo("서울시 종로구");
    }

    @Test
    @DisplayName("발주 요청 시 배송 필드가 있으면 OrderRequest 스냅샷에 그대로 저장된다")
    void submitOrderRequest_usesOverrideShippingWhenProvided() {
        Long userId = 1L;
        OrderCreateRequest request = OrderCreateRequest.builder()
                .cartDetailIds(List.of(10L))
                .receiverName("별도수령인")
                .receiverPhone("010-9999-8888")
                .destinationAddress("경기 성남시 분당구")
                .build();

        Store store = Store.builder().name("본점").address("서울시 종로구").build();
        User user = User.builder().id(userId).name("김직원").phone("010-2222-3333").store(store).build();
        Cart cart = Cart.builder().cartId(1L).user(user).build();
        Item item = Item.builder().itemId(100L).unitPrice(2000L).currentStock(50).build();
        CartDetail cartDetail = CartDetail.builder().cart(cart).item(item).quantity(1).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartDetailRepository.findWithCartAndUserByIds(request.getCartDetailIds())).willReturn(List.of(cartDetail));
        given(itemRepository.findByIdWithLock(100L)).willReturn(Optional.of(item));

        orderEmpService.submitOrderRequest(userId, request);

        ArgumentCaptor<OrderRequest> orderCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderRequestRepository).save(orderCaptor.capture());
        OrderRequest saved = orderCaptor.getValue();
        assertThat(saved.getReceiverName()).isEqualTo("별도수령인");
        assertThat(saved.getReceiverPhone()).isEqualTo("010-9999-8888");
        assertThat(saved.getDestinationAddress()).isEqualTo("경기 성남시 분당구");
    }

    @Test
    @DisplayName("발주 요청 시 주소만 비어 있으면 소속 매장 주소를 기본값으로 저장한다")
    void submitOrderRequest_partialOverride_usesStoreAddressForBlankDestination() {
        Long userId = 1L;
        OrderCreateRequest request = OrderCreateRequest.builder()
                .cartDetailIds(List.of(10L))
                .receiverName("현장인수자")
                .receiverPhone("010-1111-2222")
                .build();

        Store store = Store.builder().name("창고").address("인천 연수구").build();
        User user = User.builder().id(userId).name("김직원").phone("010-2222-3333").store(store).build();
        Cart cart = Cart.builder().cartId(1L).user(user).build();
        Item item = Item.builder().itemId(100L).unitPrice(2000L).currentStock(50).build();
        CartDetail cartDetail = CartDetail.builder().cart(cart).item(item).quantity(1).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(cartDetailRepository.findWithCartAndUserByIds(request.getCartDetailIds())).willReturn(List.of(cartDetail));
        given(itemRepository.findByIdWithLock(100L)).willReturn(Optional.of(item));

        orderEmpService.submitOrderRequest(userId, request);

        ArgumentCaptor<OrderRequest> orderCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderRequestRepository).save(orderCaptor.capture());
        OrderRequest saved = orderCaptor.getValue();
        assertThat(saved.getReceiverName()).isEqualTo("현장인수자");
        assertThat(saved.getReceiverPhone()).isEqualTo("010-1111-2222");
        assertThat(saved.getDestinationAddress()).isEqualTo("인천 연수구");
    }

    @Test
    @DisplayName("발주 조회 성공 - '상품명 외 N건' 형태로 대표 이름이 잘 만들어진다")
    void getMyOrderHistory_Success() {
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
                .orderDetails(List.of(d1, d2, d3))
                .status(OrderStatus.PAID)
                .build();

        given(orderRequestRepository.findAllByRequestUser_IdOrderByRequestDateDesc(1L))
                .willReturn(List.of(order));

        List<OrderListResponse> result = orderEmpService.getMyOrderHistory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRepresentativeItemName()).isEqualTo("키보드 외 2건");
    }

    @Test
    @DisplayName("발주 취소 성공 - 상태가 취소로 변경되고 처리 시각이 기록된다")
    void cancelOrder_Success() {
        User user = User.builder().id(1L).build();

        OrderRequest order = OrderRequest.builder()
                .requestUser(user)
                .status(OrderStatus.REQUESTED)
                .build();

        given(orderRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(order));

        orderEmpService.cancelOrder(1L, 100L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getProcessDate()).isNotNull();
    }

    @Test
    @DisplayName("발주 취소 실패 - 이미 처리 중이거나 완료된 주문은 취소 불가")
    void cancelOrder_Fail_InvalidStatus() {
        User user = User.builder().id(1L).build();
        OrderRequest order = OrderRequest.builder()
                .requestUser(user)
                .status(OrderStatus.PARTIAL)
                .build();

        given(orderRequestRepository.findByIdForUpdate(100L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderEmpService.cancelOrder(1L, 100L))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("이미 처리 진행 중이거나 완료된 주문");
    }
}
