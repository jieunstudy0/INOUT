package com.jstudy.inout.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.jstudy.inout.common.config.JpaAuditConfig;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.entity.StockReceivingHistory;
import com.jstudy.inout.stock.entity.StockUsageHistory;
import com.jstudy.inout.stock.testsupport.StockJpaTestApplication;

@DataJpaTest
@ActiveProfiles("jpa-slice")
@ContextConfiguration(classes = StockJpaTestApplication.class)
@Import(JpaAuditConfig.class)
class StockHistoryRepositoryTest {

    @Autowired private StockReceivingHistoryRepository receivingRepo;
    @Autowired private StockUsageHistoryRepository usageRepo;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemCategoryRepository categoryRepository;

    private User testUser;
    private Store testStore;
    private Item testItem;

    @BeforeEach
    void setUp() {
        testStore = Store.builder().name("종로점").address("서울").build();
        storeRepository.save(testStore);

        testUser = User.builder()
                .email("emp@inout.com").password("1234").name("김직원")
                .phone("010-1111").store(testStore).birthday(LocalDate.now())
                .build();
        userRepository.save(testUser);

        ItemCategory category = ItemCategory.builder().categoryName("사무용품").build();
        categoryRepository.save(category);

        testItem = Item.builder().name("A4용지").category(category).currentStock(100).minStockLevel(0).unitPrice(5000L).deleted(false).build();
        itemRepository.save(testItem);

        StockReceivingHistory receive = StockReceivingHistory.builder()
                .item(testItem).user(testUser).receivingQuantity(50).resultStock(150).build();
        receivingRepo.save(receive);

        StockUsageHistory usage = StockUsageHistory.builder()
                .item(testItem).user(testUser).usageQuantity(10).resultStock(140).build();
        usageRepo.save(usage);
    }

    @Test
    @DisplayName("매장 ID(StoreId)를 통해 해당 매장 소속 직원의 입고/사용 이력을 모두 조회한다")
    void findAllByUser_Store_StoreId() {
        // when
        List<StockReceivingHistory> receiveList = receivingRepo.findAllByUser_Store_Id(testStore.getId());
        List<StockUsageHistory> usageList = usageRepo.findAllByUser_Store_Id(testStore.getId());

        // then
        assertThat(receiveList).hasSize(1);
        assertThat(receiveList.get(0).getReceivingQuantity()).isEqualTo(50);

        assertThat(usageList).hasSize(1);
        assertThat(usageList.get(0).getUsageQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("아이템 ID(ItemId)를 통해 특정 상품의 이력을 조회한다")
    void findAllByItem_ItemId() {
        // when
        List<StockReceivingHistory> receiveList = receivingRepo.findAllByItem_ItemId(testItem.getItemId());

        // then
        assertThat(receiveList).hasSize(1);
        assertThat(receiveList.get(0).getItem().getName()).isEqualTo("A4용지");
    }
}