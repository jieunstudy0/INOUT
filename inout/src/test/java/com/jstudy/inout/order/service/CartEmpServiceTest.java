package com.jstudy.inout.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.jstudy.inout.order.dto.CartAddRequest;
import com.jstudy.inout.order.dto.CartResponse;
import com.jstudy.inout.order.entity.Cart;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.CartDetailRepository;
import com.jstudy.inout.order.repository.CartRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;

@ExtendWith(MockitoExtension.class)
class CartEmpServiceTest {

    @InjectMocks
    private CartEmpService cartEmpService;

    @Mock private CartDetailRepository cartDetailRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private OrderRequestRepository orderRequestRepository;

    @Test
    @DisplayName("장바구니 담기 성공 - 기존에 담긴 상품이면 수량만 증가한다")
    void addToCart_Success_ExistingItem() {
        Long userId = 1L;
        CartAddRequest request = new CartAddRequest(100L, 5);

        Cart cart = Cart.builder().cartId(1L).build();
        Item item = Item.builder().itemId(100L).build();

        CartDetail existingDetail = CartDetail.builder()
                .cartDetailId(10L)
                .cart(cart)
                .item(item)
                .quantity(2)
                .build();

        given(cartRepository.findByUser_Id(userId)).willReturn(Optional.of(cart));
        given(itemRepository.findById(100L)).willReturn(Optional.of(item));
        given(cartDetailRepository.findByCartAndItem(cart, item)).willReturn(Optional.of(existingDetail));

        cartEmpService.addToCart(userId, request);

        assertThat(existingDetail.getQuantity()).isEqualTo(7);
        verify(cartDetailRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("재주문 성공 - 이전 주문 내역을 장바구니에 그대로 담는다 (단종/품절 제외)")
    void reOrder_Success() {
        Long userId = 1L;
        Long pastOrderId = 200L;

        User user = User.builder().id(userId).build();
        Cart cart = Cart.builder().cartId(1L).build();

        Item validItem = Item.builder().itemId(10L).deleted(false).currentStock(50).build();
        Item outOfStockItem = Item.builder().itemId(11L).deleted(false).currentStock(0).build();

        OrderDetail validDetail = OrderDetail.builder().item(validItem).requestQuantity(3).build();
        OrderDetail outOfStockDetail = OrderDetail.builder().item(outOfStockItem).requestQuantity(2).build();

        OrderRequest pastOrder = OrderRequest.builder()
                .id(pastOrderId)
                .requestUser(user)
                .orderDetails(List.of(validDetail, outOfStockDetail))
                .build();

        given(orderRequestRepository.findWithDetailsGraphById(pastOrderId)).willReturn(Optional.of(pastOrder));
        given(cartRepository.findByUser_Id(userId)).willReturn(Optional.of(cart));
        given(cartDetailRepository.findByCartAndItem(any(), any())).willReturn(Optional.empty());

        cartEmpService.reOrder(userId, pastOrderId);

        verify(cartDetailRepository, times(1)).save(any(CartDetail.class));
    }

    @Test
    @DisplayName("재주문 실패 - 본인의 주문이 아니면 에러 발생")
    void reOrder_Fail_Forbidden() {
        User pastOrderUser = User.builder().id(2L).build();
        OrderRequest pastOrder = OrderRequest.builder().requestUser(pastOrderUser).build();

        given(orderRequestRepository.findWithDetailsGraphById(200L)).willReturn(Optional.of(pastOrder));

        assertThatThrownBy(() -> cartEmpService.reOrder(1L, 200L))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("본인의 주문 내역만");
    }

    @Test
    @DisplayName("장바구니 수량 수정 성공")
    void updateQuantity_Success() {
        User user = User.builder().id(1L).build();
        Cart cart = Cart.builder().user(user).build();
        CartDetail cartDetail = CartDetail.builder().cartDetailId(10L).cart(cart).quantity(2).build();

        given(cartDetailRepository.findById(10L)).willReturn(Optional.of(cartDetail));

        cartEmpService.updateQuantity(1L, 10L, 10);

        assertThat(cartDetail.getQuantity()).isEqualTo(10);
    }
}
