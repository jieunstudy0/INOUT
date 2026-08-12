package com.jstudy.inout.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jstudy.inout.common.config.CacheConfig;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse.ActivityItem;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardAggregateService {

    private final ItemRepository itemRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final InquiryRepository inquiryRepository;
    private final StockReceivingHistoryRepository receivingRepository;
    private final StockUsageHistoryRepository usageRepository;
    private final DeliveryRepository deliveryRepository;

    private static final DateTimeFormatter FEED_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Cacheable(value = CacheConfig.DASHBOARD_SUMMARY, key = "'admin'")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getCachedAggregateSummary() {

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long normalStockCount      = itemRepository.countNormalStockItems();
        long lowStockCount         = itemRepository.countLowStockItems();
        long outOfStockCount       = itemRepository.countOutOfStockItems();
        long totalActiveStockCount = itemRepository.countActiveItems();
        long todayNewOrderCount  = orderRequestRepository.countTodayOrders(startOfDay);
        long pendingOrderCount   = orderRequestRepository.countByStatus(OrderStatus.ORDERED);
        long completedOrderCount = orderRequestRepository.countByStatus(OrderStatus.APPROVED);
        long rejectedOrderCount  = orderRequestRepository.countByStatus(OrderStatus.REJECTED);
        long totalOrderCount     = orderRequestRepository.count();
        Long rawOrderAmount = orderRequestRepository.sumTodayOrderAmount(
                startOfDay,
                List.of(OrderStatus.ORDERED, OrderStatus.APPROVED, OrderStatus.PARTIAL, OrderStatus.PAID, OrderStatus.COMPLETED));
        long todayOrderAmount = rawOrderAmount != null ? rawOrderAmount : 0L;
        long pendingDeliveryCount = deliveryRepository.countByStatus(DeliveryStatus.READY);
        long shippingDeliveryCount = deliveryRepository.countByStatus(DeliveryStatus.SHIPPING);
        long completedDeliveryCount = deliveryRepository.countByStatus(DeliveryStatus.COMPLETED);

        int todayInCount  = receivingRepository.countByProcessDateAfter(startOfDay);
        int todayOutCount = usageRepository.countByProcessDateAfter(startOfDay);
        long unreadInquiryCount = inquiryRepository.countByIsReadFalse();
        long waitingCsInquiryCount = unreadInquiryCount;
        long aiDraftCompletedCount = inquiryRepository.countByAiDraftAnswerIsNotNull();
        long aiSuggestedPendingOrderCount = orderDetailRepository.countByIsAiSuggestedTrueAndStatus(OrderDetailStatus.WAITING);

        List<ActivityItem> activities = buildRecentActivities();
      
        return DashboardSummaryResponse.builder()
                .todayNewOrderCount(todayNewOrderCount)
                .lowStockCount(lowStockCount)
                .todayOrderAmount(todayOrderAmount)
                .pendingDeliveryCount(pendingDeliveryCount)
                .shippingDeliveryCount(shippingDeliveryCount)    
                .completedDeliveryCount(completedDeliveryCount)   
                .normalStockCount(normalStockCount)
                .outOfStockCount(outOfStockCount)
                .totalActiveStockCount(totalActiveStockCount)
                .pendingOrderCount(pendingOrderCount)
                .completedOrderCount(completedOrderCount)
                .rejectedOrderCount(rejectedOrderCount)
                .totalOrderCount(totalOrderCount)
                .todayInCount(todayInCount)
                .todayOutCount(todayOutCount)
                .unreadInquiryCount(unreadInquiryCount)
                .waitingCsInquiryCount(waitingCsInquiryCount)
                .aiDraftCompletedCount(aiDraftCompletedCount)
                .aiSuggestedPendingOrderCount(aiSuggestedPendingOrderCount)
                .recentActivities(activities)
                .build();
    }

    @CacheEvict(value = CacheConfig.DASHBOARD_SUMMARY, key = "'admin'")
    public void evictCachedAggregateSummary() {
    }

    private List<ActivityItem> buildRecentActivities() {
        List<ActivityItem> activities = new ArrayList<>();


        orderRequestRepository.findRecentOrdersByStatus(OrderStatus.REJECTED, PageRequest.of(0, 2))
        .forEach(o -> {
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

        itemRepository.findLowStockItemsAboveZero()
                .stream()
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
}
