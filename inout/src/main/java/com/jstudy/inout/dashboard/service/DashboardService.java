package com.jstudy.inout.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.config.CacheConfig;
import com.jstudy.inout.dashboard.dto.DashboardStatisticsResponse;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse.ActivityItem;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ItemRepository itemRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final InquiryRepository inquiryRepository;
    private final StockReceivingHistoryRepository receivingRepository;
    private final StockUsageHistoryRepository usageRepository;
    private final DeliveryRepository deliveryRepository;
    private final DashboardAggregateService dashboardAggregateService;

    private static final DateTimeFormatter FEED_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(User user) {
        DashboardSummaryResponse aggregate = dashboardAggregateService.getCachedAggregateSummary();

        String storeName = (user.getStore() != null) ? user.getStore().getName() : "지점 미지정";
        
        return DashboardSummaryResponse.builder()
                .userName(user.getName())
                .storeName(storeName)
                .todayNewOrderCount(aggregate.getTodayNewOrderCount())
                .lowStockCount(aggregate.getLowStockCount())
                .todayOrderAmount(aggregate.getTodayOrderAmount())
                .pendingDeliveryCount(aggregate.getPendingDeliveryCount())
                .shippingDeliveryCount(aggregate.getShippingDeliveryCount())
                .completedDeliveryCount(aggregate.getCompletedDeliveryCount())
                .normalStockCount(aggregate.getNormalStockCount())
                .outOfStockCount(aggregate.getOutOfStockCount())
                .totalActiveStockCount(aggregate.getTotalActiveStockCount())
                .pendingOrderCount(aggregate.getPendingOrderCount())
                .completedOrderCount(aggregate.getCompletedOrderCount())
                .rejectedOrderCount(aggregate.getRejectedOrderCount())
                .totalOrderCount(aggregate.getTotalOrderCount())
                .todayInCount(aggregate.getTodayInCount())
                .todayOutCount(aggregate.getTodayOutCount())
                .unreadInquiryCount(aggregate.getUnreadInquiryCount())
                .waitingCsInquiryCount(aggregate.getWaitingCsInquiryCount())
                .aiDraftCompletedCount(aggregate.getAiDraftCompletedCount())
                .aiSuggestedPendingOrderCount(aggregate.getAiSuggestedPendingOrderCount())
                .recentActivities(aggregate.getRecentActivities())
                .build();
    }

    private List<ActivityItem> buildRecentActivities() {
        List<ActivityItem> activities = new ArrayList<>();

        orderRequestRepository.findAllWithDetailsByStatusOrderByDateDesc(OrderStatus.REJECTED)
        .stream().limit(2).forEach(o -> {
            String storeName = (o.getRequestUser() != null && o.getRequestUser().getStore() != null)
                               ? o.getRequestUser().getStore().getName() : "알 수 없는 지점";
            activities.add(ActivityItem.builder()
                    .type("ORDER_REJECTED")
                    .message("재고 부족으로 주문 #" + o.getId() + "이 자동 반려되었습니다. (" + storeName + ")")
                    .time(o.getRequestDate() != null ? o.getRequestDate().format(FEED_FMT) : "-")
                    .severity("danger")
                    .build());
        });

        itemRepository.findOutOfStockItems()
                .stream()
                .limit(3)
                .forEach(item -> activities.add(ActivityItem.builder()
                        .type("LOW_STOCK")
                        .message("'" + item.getName() + "' 상품이 품절 상태입니다.")
                        .time("-")
                        .severity("warning")
                        .build()));

        itemRepository.findLowStockItems()
                .stream()
                .filter(i -> i.getCurrentStock() > 0)
                .limit(2)
                .forEach(item -> activities.add(ActivityItem.builder()
                        .type("LOW_STOCK")
                        .message("'" + item.getName() + "' 재고가 안전 재고 미만입니다. (현재 "
                                 + item.getCurrentStock() + "/" + item.getMinStockLevel() + ")")
                        .time("-")
                        .severity("warning")
                        .build()));

        orderRequestRepository.findRecentOrders(PageRequest.of(0, 3))
        .forEach(o -> {
            String storeName = (o.getRequestUser() != null && o.getRequestUser().getStore() != null)
                               ? o.getRequestUser().getStore().getName() : "알 수 없는 지점";
            activities.add(ActivityItem.builder()
                    .type("ORDER_IN")
                    .message(storeName + "에서 발주가 접수되었습니다. (#" + o.getId() + ")")
                    .time(o.getRequestDate() != null ? o.getRequestDate().format(FEED_FMT) : "-")
                    .severity("info")
                    .build());
        });

        return activities.stream().limit(10).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getDashboardStatistics() {

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Object[]> monthlyData = orderRequestRepository.findMonthlyOrderAmountTrend(sixMonthsAgo);
        List<DashboardStatisticsResponse.MonthlyTrend> monthlyTrends = monthlyData.stream()
                .map(row -> DashboardStatisticsResponse.MonthlyTrend.builder()
                        .month(String.format("%04d-%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                        .totalAmount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        List<Object[]> storeData = orderRequestRepository.findTopStoreOrderFrequency(oneMonthAgo, PageRequest.of(0, 5));
        List<DashboardStatisticsResponse.StoreFrequency> storeFrequencies = storeData.stream()
                .map(row -> DashboardStatisticsResponse.StoreFrequency.builder()
                        .storeName((String) row[0])
                        .orderCount(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        List<Object[]> itemData = usageRepository.findTopConsumedItems(oneMonthAgo, PageRequest.of(0, 5));
        List<DashboardStatisticsResponse.ItemConsumption> topConsumedItems = itemData.stream()
                .map(row -> DashboardStatisticsResponse.ItemConsumption.builder()
                        .itemName((String) row[0])
                        .totalConsumed(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return DashboardStatisticsResponse.builder()
                .monthlyTrends(monthlyTrends)
                .storeFrequencies(storeFrequencies)
                .topConsumedItems(topConsumedItems)
                .build();
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY, allEntries = true)
    public void evictDashboardSummary() {
        try {
            dashboardAggregateService.evictCachedAggregateSummary();
        } catch (RuntimeException ex) {
            // CacheErrorHandler / ResilientCacheManager가 대부분의 경우를 삼키지만,
            // 프록시·직접 호출 경로에서의 예외가 호출부(AI 저장 등)로 전파되지 않게 이중 방어한다.
            log.warn("[Dashboard] 집계 캐시 무효화 실패(무시). cause={}", ex.getMessage());
        }
    }
}