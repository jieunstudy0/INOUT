package com.jstudy.inout.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.CacheConfig;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
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
@ContextConfiguration(classes = DashboardServiceCacheTest.CacheTestConfig.class)
class DashboardServiceCacheTest {

    @Configuration
    @EnableCaching
    @Import({DashboardService.class, DashboardAggregateService.class})
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheConfig.DASHBOARD_SUMMARY);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
            given(txManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
            return txManager;
        }

        @Bean ItemRepository itemRepository()                           { return mock(ItemRepository.class); }
        @Bean OrderRequestRepository orderRequestRepository()           { return mock(OrderRequestRepository.class); }
        @Bean OrderDetailRepository orderDetailRepository()             { return mock(OrderDetailRepository.class); }
        @Bean InquiryRepository inquiryRepository()                     { return mock(InquiryRepository.class); }
        @Bean StockReceivingHistoryRepository receivingRepository()     { return mock(StockReceivingHistoryRepository.class); }
        @Bean StockUsageHistoryRepository usageRepository()             { return mock(StockUsageHistoryRepository.class); }
        @Bean DeliveryRepository deliveryRepository()                   { return mock(DeliveryRepository.class); }
    }

    @Autowired DashboardService         dashboardService;
    @Autowired CacheManager             cacheManager;
    @Autowired ItemRepository           itemRepository;
    @Autowired OrderRequestRepository   orderRequestRepository;
    @Autowired OrderDetailRepository    orderDetailRepository;
    @Autowired InquiryRepository        inquiryRepository;
    @Autowired StockReceivingHistoryRepository receivingRepository;
    @Autowired StockUsageHistoryRepository    usageRepository;
    @Autowired DeliveryRepository       deliveryRepository;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).name("관리자").build();

        cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY).clear();
        reset(itemRepository, orderRequestRepository, orderDetailRepository, inquiryRepository,
              receivingRepository, usageRepository, deliveryRepository);

        given(itemRepository.findLowStockItems()).willReturn(List.of());
        given(itemRepository.findOutOfStockItems()).willReturn(List.of());
        given(orderRequestRepository.findAllWithDetailsByStatusOrderByDateDesc(any())).willReturn(List.of());
        given(orderRequestRepository.findRecentOrders(any())).willReturn(List.of());
    }


    @Test
    @DisplayName("첫 번째 호출은 DB를 조회하고, 두 번째 호출은 캐시에서 반환하여 DB를 호출하지 않는다")
    void getDashboardSummary_secondCall_hitsCache_skipsDb() {
        // when
        DashboardSummaryResponse first  = dashboardService.getDashboardSummary(admin);
        DashboardSummaryResponse second = dashboardService.getDashboardSummary(admin);

        // then
        verify(itemRepository, times(1)).countNormalStockItems();
        verify(itemRepository, times(1)).countLowStockItems();
        verify(inquiryRepository, times(1)).countByIsReadFalse();
        verify(orderRequestRepository, times(1)).count();  

        assertThat(second).isNotSameAs(first);
        assertThat(second.getTotalOrderCount()).isEqualTo(first.getTotalOrderCount());
        assertThat(second.getUserName()).isEqualTo(admin.getName());
    }

    @Test
    @DisplayName("evictDashboardSummary 호출 후에는 다음 요청이 DB를 다시 조회한다")
    void evictDashboardSummary_clearsCache_nextCallHitsDb() {
        // given
        dashboardService.getDashboardSummary(admin);
        verify(inquiryRepository, times(1)).countByIsReadFalse();

        // when
        dashboardService.evictDashboardSummary();

        // then
        dashboardService.getDashboardSummary(admin);
        verify(inquiryRepository, times(2)).countByIsReadFalse(); 
    }

    @Test
    @DisplayName("캐시 키 'admin'은 관리자 ID에 무관하게 단일 캐시 엔트리를 공유하지만, 개인화 정보는 요청자별로 정확히 채워진다")
    void getDashboardSummary_sharedCacheKey_singleEntryForAllAdmins() {
        // given
        User adminA = User.builder().id(1L).name("관리자A").build();
        User adminB = User.builder().id(2L).name("관리자B").build();

        // when
        DashboardSummaryResponse responseA = dashboardService.getDashboardSummary(adminA);
        DashboardSummaryResponse responseB = dashboardService.getDashboardSummary(adminB);

        // then
        verify(inquiryRepository, times(1)).countByIsReadFalse();
        
        assertThat(responseA.getUserName()).isEqualTo("관리자A");
        assertThat(responseB.getUserName()).isEqualTo("관리자B");
    }

    @Test
    @DisplayName("캐시가 정상 동작할 때 Cache 엔트리가 실제로 저장된다")
    void getDashboardSummary_storesCacheEntry_afterFirstCall() {
        // when
        dashboardService.getDashboardSummary(admin);

        // then
        Cache cache = cacheManager.getCache(CacheConfig.DASHBOARD_SUMMARY);
        assertThat(cache).isNotNull();
        assertThat(cache.get("admin")).isNotNull();
        assertThat(cache.get("admin").get()).isInstanceOf(DashboardSummaryResponse.class);
    }
}
