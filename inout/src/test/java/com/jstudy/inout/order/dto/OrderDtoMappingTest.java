package com.jstudy.inout.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.stock.entity.Item;

class OrderDtoMappingTest {

    @Test
    @DisplayName("OrderPreResponse 변환 시 유저 정보와 장바구니 총 수량/금액이 정확히 계산된다")
    void orderPreResponse_MappingAndCalculation() {
        // given
        Store store = Store.builder().name("강남점").address("서울 강남구").build();
        User user = User.builder().name("김직원").store(store).build();

        Item item1 = Item.builder().name("마우스").unitPrice(20000L).build();
        Item item2 = Item.builder().name("키보드").unitPrice(50000L).build();

        CartDetail detail1 = CartDetail.builder().cartDetailId(1L).item(item1).quantity(2).build(); 
        CartDetail detail2 = CartDetail.builder().cartDetailId(2L).item(item2).quantity(1).build(); 

        // when
        OrderPreResponse response = OrderPreResponse.from(user, List.of(detail1, detail2));

        // then
        assertThat(response.getStoreName()).isEqualTo("강남점");
        assertThat(response.getEmployeeName()).isEqualTo("김직원");
        assertThat(response.getTotalQuantity()).isEqualTo(3); 
        assertThat(response.getTotalPrice()).isEqualTo(90000L); 
    }

    @Test
    @DisplayName("CartItemResponse 변환 시 개별 항목의 subTotal(단가*수량)이 정확히 계산된다")
    void cartItemResponse_SubTotalCalculation() {
        // given
        Item item = Item.builder().name("모니터").unitPrice(150000L).build();
        CartDetail cartDetail = CartDetail.builder().cartDetailId(10L).item(item).quantity(2).build();

        // when
        CartResponse.CartItemResponse response = CartResponse.CartItemResponse.from(cartDetail);

        // then
        assertThat(response.getItemName()).isEqualTo("모니터");
        assertThat(response.getUnitPrice()).isEqualTo(150000L);
        assertThat(response.getSubTotal()).isEqualTo(300000L);
    }
}