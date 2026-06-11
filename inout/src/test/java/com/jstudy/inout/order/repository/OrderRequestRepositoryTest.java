package com.jstudy.inout.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.order.testsupport.OrderJpaTestApplication;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = OrderJpaTestApplication.class)
@Import(JpaAuditConfig.class)
class OrderRequestRepositoryTest {

    @Autowired private OrderRequestRepository orderRequestRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private TestEntityManager em;

    private User testUser;
    private OrderRequest testOrder;

    @BeforeEach
    void setUp() {
        Store store = Store.builder().name("본점").address("서울").build();
        em.persist(store);

        testUser = User.builder()
                .email("order@user.com")
                .password("encoded")
                .name("발주자")
                .phone("010-1111-2222")
                .birthday(LocalDate.of(1990, 1, 1))
                .store(store)
                .deleted(false)
                .build();
        em.persist(testUser);

        ItemCategory category = ItemCategory.builder().categoryName("사무").build();
        em.persist(category);

        Item item = Item.builder()
                .name("노트")
                .unitPrice(2000L)
                .currentStock(50)
                .deleted(false)
                .category(category)
                .minStockLevel(0)
                .build();
        em.persist(item);

        testOrder = OrderRequest.builder()
                .requestUser(testUser)
                .status(OrderStatus.REQUESTED)
                .totalPrice(10000L)
                .requestDate(LocalDateTime.now())
                .receiverName(testUser.getName())
                .receiverPhone(testUser.getPhone())
                .destinationAddress(store.getAddress())
                .build();
        orderRequestRepository.save(testOrder);

        OrderDetail detail = OrderDetail.builder()
                .orderRequest(testOrder)
                .item(item)
                .requestQuantity(5)
                .itemPriceSnapshot(2000L)
                .status(OrderDetailStatus.WAITING)
                .build();
        orderDetailRepository.save(detail);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("상태별 발주 개수를 정확히 카운트한다")
    void countByStatus() {
        long requestedCount = orderRequestRepository.countByStatus(OrderStatus.REQUESTED);
        long completedCount = orderRequestRepository.countByStatus(OrderStatus.COMPLETED);

        assertThat(requestedCount).isEqualTo(1);
        assertThat(completedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("@EntityGraph 적용 쿼리: 유저 ID로 조회 시 연관된 상세(Detail), 상품(Item), 매장(Store) 정보를 한 번에 조회한다")
    void findAllByRequestUser_IdOrderByRequestDateDesc() {
        List<OrderRequest> orders = orderRequestRepository
                .findAllByRequestUser_IdOrderByRequestDateDesc(testUser.getId());

        assertThat(orders).hasSize(1);

        OrderRequest foundOrder = orders.get(0);
        assertThat(foundOrder.getRequestUser().getStore().getName()).isEqualTo("본점");
        assertThat(foundOrder.getOrderDetails()).hasSize(1);
        assertThat(foundOrder.getOrderDetails().get(0).getItem().getName()).isEqualTo("노트");
    }

    @Test
    @DisplayName("@EntityGraph 커스텀 쿼리: 상태값으로 발주와 상세 정보를 함께 조회한다")
    void findAllWithDetailsByStatus() {
        List<OrderRequest> orders = orderRequestRepository.findAllWithDetailsByStatus(OrderStatus.REQUESTED);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(orders.get(0).getRequestUser().getName()).isEqualTo("발주자");
    }
}
