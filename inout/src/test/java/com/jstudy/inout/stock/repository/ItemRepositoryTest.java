package com.jstudy.inout.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.testsupport.StockJpaTestApplication;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = StockJpaTestApplication.class)
@Import(JpaAuditConfig.class)
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemCategoryRepository itemCategoryRepository;

    private Item normalItem;
    private Item lowStockItem;
    private Item outOfStockItem;
    private Item deletedItem;

    @BeforeEach
    void setUp() {
        ItemCategory category = ItemCategory.builder().categoryName("문구").build();
        itemCategoryRepository.save(category);
        normalItem = Item.builder().name("모나미볼펜").category(category).currentStock(50).minStockLevel(10).deleted(false).unitPrice(1000L).build();
        lowStockItem = Item.builder().name("플러스펜").category(category).currentStock(5).minStockLevel(10).deleted(false).unitPrice(1200L).build(); // 저재고 (5 <= 10)
        outOfStockItem = Item.builder().name("지우개").category(category).currentStock(0).minStockLevel(5).deleted(false).unitPrice(500L).build(); // 품절 (0)
        deletedItem = Item.builder().name("단종펜").category(category).currentStock(2).minStockLevel(10).deleted(true).unitPrice(2000L).build(); // 논리 삭제됨

        itemRepository.saveAll(List.of(normalItem, lowStockItem, outOfStockItem, deletedItem));
    }

    @Test
    @DisplayName("커스텀 쿼리: 삭제되지 않은 저재고(minStockLevel 이하) 상품만 조회된다")
    void findLowStockItems() {
        List<Item> lowStockItems = itemRepository.findLowStockItems();

        // then
        assertThat(lowStockItems).hasSize(2);
        assertThat(lowStockItems).extracting(Item::getName)
                .containsExactlyInAnyOrder("플러스펜", "지우개");
    }

    @Test
    @DisplayName("커스텀 쿼리: 삭제되지 않은 품절(재고 0) 상품만 조회된다")
    void findOutOfStockItems() {
        List<Item> outOfStockItems = itemRepository.findOutOfStockItems();

        // then
        assertThat(outOfStockItems).hasSize(1);
        assertThat(outOfStockItems.get(0).getName()).isEqualTo("지우개");
    }

    @Test
    @DisplayName("이름 포함 검색 및 삭제 여부로 페이징 조회가 가능하다")
    void findByNameContainingAndDeleted() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<Item> result = itemRepository.findByNameContainingAndDeleted("펜", false, pageRequest);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Item::getName)
                .containsExactlyInAnyOrder("모나미볼펜", "플러스펜");
    }
}