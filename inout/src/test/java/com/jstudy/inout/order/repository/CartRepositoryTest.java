package com.jstudy.inout.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.order.testsupport.OrderJpaTestApplication;
import com.jstudy.inout.order.entity.Cart;
import com.jstudy.inout.order.entity.CartDetail;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = OrderJpaTestApplication.class)
@Import(JpaAuditConfig.class)
class CartRepositoryTest {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartDetailRepository cartDetailRepository;

    @Autowired private TestEntityManager em;

    private User testUser;
    private Cart testCart;
    private Item testItem;
    private CartDetail testCartDetail;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@user.com")
                .password("encoded")
                .name("직원")
                .phone("010-3333-4444")
                .birthday(LocalDate.of(1992, 5, 5))
                .deleted(false)
                .build();
        em.persist(testUser);

        ItemCategory category = ItemCategory.builder().categoryName("테스트카테고리").build();
        em.persist(category);

        testItem = Item.builder()
                .name("테스트상품")
                .unitPrice(1000L)
                .currentStock(10)
                .deleted(false)
                .category(category)
                .minStockLevel(0)
                .build();
        em.persist(testItem);

        testCart = Cart.builder().user(testUser).build();
        cartRepository.save(testCart);

        testCartDetail = CartDetail.builder()
                .cart(testCart)
                .item(testItem)
                .quantity(5)
                .deleted(false)
                .build();
        cartDetailRepository.save(testCartDetail);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("유저 ID로 장바구니를 성공적으로 조회한다")
    void findByUser_Id() {
        Optional<Cart> foundCart = cartRepository.findByUser_Id(testUser.getId());

        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getUser().getName()).isEqualTo("직원");
    }

    @Test
    @DisplayName("JOIN FETCH 쿼리 검증: IN 절로 CartDetail 조회 시 연관된 Cart, User를 함께 가져온다")
    void findWithCartAndUserByIds() {
        List<CartDetail> details = cartDetailRepository.findWithCartAndUserByIds(List.of(testCartDetail.getCartDetailId()));

        assertThat(details).hasSize(1);
        assertThat(details.get(0).getCart().getUser().getEmail()).isEqualTo("test@user.com");
    }

    @Test
    @DisplayName("@Modifying 벌크 연산: 리스트로 전달된 ID들의 deleted 상태를 일괄 true로 변경한다")
    void updateDeletedStatusInBatch() {
        cartDetailRepository.updateDeletedStatusInBatch(List.of(testCartDetail.getCartDetailId()));

        em.flush();
        em.clear();

        CartDetail updated = cartDetailRepository.findById(testCartDetail.getCartDetailId()).orElseThrow();
        assertThat(updated.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("@Modifying 벌크 연산: 특정 유저의 모든 장바구니 상세 항목을 논리 삭제한다")
    void updateAllDeletedStatusByUserId() {
        cartDetailRepository.updateAllDeletedStatusByUserId(testUser.getId());
        em.flush();
        em.clear();

        CartDetail updated = cartDetailRepository.findById(testCartDetail.getCartDetailId()).orElseThrow();
        assertThat(updated.isDeleted()).isTrue();
    }
}
