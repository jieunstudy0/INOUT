package com.jstudy.inout.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.dashboard.dto.DashboardSummaryResponse;
import com.jstudy.inout.inquiry.repository.InquiryRepository;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockReceivingHistoryRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ItemRepository itemRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final InquiryRepository inquiryRepository;
    private final StockReceivingHistoryRepository receivingRepository;
    private final StockUsageHistoryRepository usageRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(User user) {

        long lowStockCount = itemRepository.countLowStockItems();

        long pendingOrderCount = orderRequestRepository.countByStatus(OrderStatus.REQUESTED);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        
        int incomingCount = receivingRepository.countByProcessDateAfter(startOfDay);

        int outgoingCount = usageRepository.countByProcessDateAfter(startOfDay);

        return DashboardSummaryResponse.builder()
                .userName(user.getName())
                .storeName(user.getStore().getName())
                .lowStockCount(lowStockCount)
                .pendingOrderCount(pendingOrderCount)
                .unreadInquiryCount(inquiryRepository.countByIsReadFalse())
                .todayInCount(incomingCount)
                .todayOutCount(outgoingCount)
                .build();
    }
}
