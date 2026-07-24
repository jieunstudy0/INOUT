package com.jstudy.inout.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.config.CacheConfig;
import com.jstudy.inout.stock.dto.admin.StockRegister;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StockAdmServiceCacheTest.CacheTestConfig.class)
class StockAdmServiceCacheTest {

    @Configuration
    @EnableCaching
    @Import(StockAdmService.class)
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                CacheConfig.STOCK_ALERTS,
                CacheConfig.STOCK_LIST,
                CacheConfig.DASHBOARD_SUMMARY
            );
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
            given(txManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
            return txManager;
        }

        @Bean ItemRepository           itemRepository()           { return mock(ItemRepository.class); }
        @Bean ItemCategoryRepository   itemCategoryRepository()   { return mock(ItemCategoryRepository.class); }
        @Bean StockReceivingHistoryRepository receivingHistoryRepository() { return mock(StockReceivingHistoryRepository.class); }
        @Bean UserRepository           userRepository()           { return mock(UserRepository.class); }
        @Bean StockUsageHistoryRepository usageHistoryRepository() { return mock(StockUsageHistoryRepository.class); }
    }

    @Autowired StockAdmService stockAdmService;
    @Autowired CacheManager    cacheManager;
    @Autowired ItemRepository  itemRepository;
    @Autowired ItemCategoryRepository   itemCategoryRepository;
    @Autowired StockReceivingHistoryRepository receivingHistoryRepository;
    @Autowired UserRepository  userRepository;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CacheConfig.STOCK_ALERTS).clear();
        cacheManager.getCache(CacheConfig.STOCK_LIST).clear();
        cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY).clear();
        reset(itemRepository, itemCategoryRepository, receivingHistoryRepository, userRepository);
        given(itemRepository.findLowStockItems()).willReturn(List.of());
        given(itemRepository.findOutOfStockItems()).willReturn(List.of());
    }


    @Test
    @DisplayName("getLowStockAlerts: 두 번째 호출은 캐시를 반환하므로 DB를 조회하지 않는다")
    void getLowStockAlerts_secondCall_hitsCache_skipsDb() {
        // when
        stockAdmService.getLowStockAlerts();
        stockAdmService.getLowStockAlerts();

        // then
        verify(itemRepository, times(1)).findLowStockItems();
    }

    @Test
    @DisplayName("getOutOfStockItems: 두 번째 호출은 캐시를 반환하므로 DB를 조회하지 않는다")
    void getOutOfStockItems_secondCall_hitsCache_skipsDb() {
        // when
        stockAdmService.getOutOfStockItems();
        stockAdmService.getOutOfStockItems();

        // then
        verify(itemRepository, times(1)).findOutOfStockItems();
    }

    @Test
    @DisplayName("receiveStock 호출 후 getLowStockAlerts는 다시 DB를 조회한다 (STOCK_ALERTS 캐시 무효화)")
    void receiveStock_evictsStockAlertsCache_nextGetHitsDb() {
        // given
        stockAdmService.getLowStockAlerts();
        verify(itemRepository, times(1)).findLowStockItems();

        Item item = Item.builder().itemId(1L).currentStock(5).build();
        User admin = User.builder().id(1L).build();
        given(itemRepository.findByIdWithLock(1L)).willReturn(Optional.of(item));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        // when
        stockAdmService.receiveStock(1L, 10, 1L, "테스트 입고");

        // then
        stockAdmService.getLowStockAlerts();
        verify(itemRepository, times(2)).findLowStockItems();
    }

    @Test
    @DisplayName("registerStock 호출 시 STOCK_ALERTS와 STOCK_LIST 캐시가 동시에 무효화된다")
    void registerStock_evictsBothStockAlertsAndStockList() {
        // given
        Cache alertsCache = cacheManager.getCache(CacheConfig.STOCK_ALERTS);
        Cache listCache   = cacheManager.getCache(CacheConfig.STOCK_LIST);
        alertsCache.put("lowStock",   List.of());
        alertsCache.put("outOfStock", List.of());
        listCache.put("someListKey",  List.of());

        assertThat(alertsCache.get("lowStock")).isNotNull();
        assertThat(listCache.get("someListKey")).isNotNull();

        StockRegister request = StockRegister.builder()
                .name("신규상품")
                .categoryId(1)
                .unitPrice(1000L)
                .build();
        ItemCategory category = ItemCategory.builder().categoryId(1).categoryName("문구류").build();
        Item savedItem = Item.builder().itemId(99L).name("신규상품").category(category).build();

        given(itemCategoryRepository.findById(1)).willReturn(Optional.of(category));
        given(itemRepository.existsByName("신규상품")).willReturn(false);
        given(itemRepository.save(any(Item.class))).willReturn(savedItem);

        // when
        stockAdmService.registerStock(request);

        // then
        assertThat(alertsCache.get("lowStock")).isNull();
        assertThat(alertsCache.get("outOfStock")).isNull();
        assertThat(listCache.get("someListKey")).isNull();
    }

    @Test
    @DisplayName("registerStock 호출 시 DASHBOARD_SUMMARY 캐시도 함께 무효화된다")
    void registerStock_evictsDashboardSummaryCache() {
        // Given
        Cache dashboardCache = cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY);
        dashboardCache.put("admin", "cached-dashboard-data");

        assertThat(dashboardCache.get("admin")).isNotNull();

        StockRegister request = StockRegister.builder()
                .name("대시보드캐시테스트상품")
                .categoryId(1)
                .unitPrice(500L)
                .build();
        ItemCategory category = ItemCategory.builder().categoryId(1).categoryName("소모품").build();
        Item savedItem = Item.builder().itemId(100L).name("대시보드캐시테스트상품").category(category).build();

        given(itemCategoryRepository.findById(1)).willReturn(Optional.of(category));
        given(itemRepository.existsByName("대시보드캐시테스트상품")).willReturn(false);
        given(itemRepository.save(any(Item.class))).willReturn(savedItem);

        // When
        stockAdmService.registerStock(request);

        // Then
        assertThat(dashboardCache.get("admin")).isNull();
    }

    @Test
    @DisplayName("receiveStock 호출 시 DASHBOARD_SUMMARY 캐시도 함께 무효화된다")
    void receiveStock_evictsDashboardSummaryCache() {
        // Given
        Cache dashboardCache = cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY);
        dashboardCache.put("admin", "cached-dashboard-data");

        Item item = Item.builder().itemId(1L).currentStock(10).build();
        User admin = User.builder().id(1L).build();
        given(itemRepository.findByIdWithLock(1L)).willReturn(Optional.of(item));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        // When
        stockAdmService.receiveStock(1L, 5, 1L, "입고 테스트");

        // Then
        assertThat(dashboardCache.get("admin")).isNull();
    }

    @Test
    @DisplayName("getLowStockAlerts와 getOutOfStockItems는 같은 STOCK_ALERTS 캐시를 사용하지만 서로 다른 키를 가진다")
    void lowStockAndOutOfStock_shareCache_withDifferentKeys() {
        // when
        stockAdmService.getLowStockAlerts();
        stockAdmService.getOutOfStockItems();

        Cache alertsCache = cacheManager.getCache(CacheConfig.STOCK_ALERTS);

        // then
        assertThat(alertsCache.get("lowStock")).isNotNull();
        assertThat(alertsCache.get("outOfStock")).isNotNull();

        verify(itemRepository, times(1)).findLowStockItems();
        verify(itemRepository, times(1)).findOutOfStockItems();
    }
}
