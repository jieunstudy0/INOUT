package com.jstudy.inout.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.testsupport.OrderJpaTestApplication;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = OrderJpaTestApplication.class)
@Import(JpaAuditConfig.class)
class DeliveryRepositoryStoreScopeTest {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private TestEntityManager em;

    private Store storeA;
    private Store storeB;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        storeA = Store.builder().name("매장A").address("서울A").build();
        storeB = Store.builder().name("매장B").address("서울B").build();
        em.persist(storeA);
        em.persist(storeB);

        userA = User.builder()
                .email("a@test.com").password("pw").name("직원A")
                .phone("010-1").birthday(LocalDate.of(1990, 1, 1))
                .store(storeA).deleted(false).build();
        userB = User.builder()
                .email("b@test.com").password("pw").name("직원B")
                .phone("010-2").birthday(LocalDate.of(1990, 1, 1))
                .store(storeB).deleted(false).build();
        em.persist(userA);
        em.persist(userB);

        em.persist(deliveryFor(userA, DeliveryStatus.SHIPPING));
        em.persist(deliveryFor(userA, DeliveryStatus.READY));
        em.persist(deliveryFor(userB, DeliveryStatus.SHIPPING));
        em.flush();
        em.clear();
    }

    private Delivery deliveryFor(User user, DeliveryStatus status) {
        OrderRequest order = OrderRequest.builder()
                .requestUser(user)
                .status(OrderStatus.COMPLETED)
                .totalPrice(1000L)
                .requestDate(LocalDateTime.now())
                .receiverName(user.getName())
                .receiverPhone(user.getPhone())
                .destinationAddress(user.getStore().getAddress())
                .build();
        em.persist(order);

        return Delivery.builder()
                .orderRequest(order)
                .status(status)
                .receiverName(user.getName())
                .receiverPhone(user.getPhone())
                .destinationAddress(user.getStore().getAddress())
                .build();
    }

    @Test
    @DisplayName("매장 스코프 배송 조회 - 해당 매장 건만 반환한다")
    void findByStoreIdWithOrder_filtersByStore() {
        // when
        Page<Delivery> page = deliveryRepository.findByStoreIdWithOrder(storeA.getId(), PageRequest.of(0, 10));

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allMatch(d -> d.getOrderRequest().getRequestUser().getStore().getId().equals(storeA.getId()));
    }

    @Test
    @DisplayName("매장+상태 카운트 - SHIPPING 건수만 집계한다")
    void countByStoreIdAndStatus() {
        // when
        long shippingA = deliveryRepository.countByStoreIdAndStatus(storeA.getId(), DeliveryStatus.SHIPPING);
        long shippingB = deliveryRepository.countByStoreIdAndStatus(storeB.getId(), DeliveryStatus.SHIPPING);

        // then
        assertThat(shippingA).isEqualTo(1);
        assertThat(shippingB).isEqualTo(1);
    }
}
