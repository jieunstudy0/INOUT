package com.jstudy.inout.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.dashboard.dto.DashboardEmpResponse;
import com.jstudy.inout.leave.service.AnnualLeaveService;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.CartDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.service.DepositEmpService;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockUsageHistoryRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) 
public class DashboardEmpService {
    
    private final UserRepository userRepository;
    private final DepositEmpService depositEmpService;
    private final AnnualLeaveService annualLeaveService;
    private final CartDetailRepository cartDetailRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final StockUsageHistoryRepository usageRepository;
    private final ItemRepository itemRepository;

    private static final DateTimeFormatter FEED_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    public DashboardEmpResponse getSummary(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long depositBalance = depositEmpService
                .getMyDepositHistory(userId, PageRequest.of(0, 1))
                .getCurrentBalance();
        int remainingLeaveDays = annualLeaveService.getRemainingLeaveDays(userId);

        int cartItemCount = cartDetailRepository.countCartItemsByUserId(userId);
        int inProgressOrderCount = (int) orderRequestRepository.countByRequestUser_IdAndStatusIn(
                userId, List.of(OrderStatus.PAID, OrderStatus.PARTIAL));
        int totalOrderCount = (int) orderRequestRepository.countByRequestUser_Id(userId);
        int completedOrderCount = (int) orderRequestRepository.countByRequestUser_IdAndStatus(userId, OrderStatus.COMPLETED);
        int rejectedOrderCount = (int) orderRequestRepository.countByRequestUser_IdAndStatus(userId, OrderStatus.REJECTED);
        int todayStockUseCount = usageRepository.countByUserIdAndProcessDateAfter(userId, startOfDay);
        long totalActiveStockCount = itemRepository.countActiveItems();
        int normalStockCount       = (int) itemRepository.countNormalStockItems();
        int lowStockCount          = (int) itemRepository.countLowStockItems();
        int outOfStockCount        = (int) itemRepository.countOutOfStockItems();

        List<DashboardEmpResponse.ActivityItem> activities = buildRecentActivities(userId);

        return DashboardEmpResponse.builder()
                .userName(user.getName())             
                .storeName(user.getStore() != null ? user.getStore().getName() : "본사")
                .depositBalance(depositBalance)
                .remainingLeaveDays(remainingLeaveDays)
                .cartItemCount(cartItemCount)
                .inProgressOrderCount(inProgressOrderCount)
                .todayStockUseCount(todayStockUseCount)
                .totalActiveStockCount(totalActiveStockCount)
                .normalStockCount(normalStockCount)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .totalOrderCount(totalOrderCount)
                .completedOrderCount(completedOrderCount)
                .rejectedOrderCount(rejectedOrderCount)
                .recentActivities(activities)
                .build();
    }

    private List<DashboardEmpResponse.ActivityItem> buildRecentActivities(Long userId) {
        List<DashboardEmpResponse.ActivityItem> activities = new ArrayList<>();

        orderRequestRepository.findAllByRequestUser_IdAndStatusInOrderByRequestDateDesc(
                userId, List.of(OrderStatus.COMPLETED, OrderStatus.REJECTED, OrderStatus.PARTIAL), PageRequest.of(0, 4))
        .forEach(o -> {
            String type;
            String message;
            String severity;

            if (o.getStatus() == OrderStatus.COMPLETED) {
                type = "ORDER_APPROVED";
                message = "주문 #" + o.getId() + " 품목이 모두 승인 및 배송 처리되었습니다.";
                severity = "success";
            } else if (o.getStatus() == OrderStatus.REJECTED) {
                type = "ORDER_REJECTED";
                message = "주문 #" + o.getId() + " 접수가 전면 반려되었습니다.";
                severity = "danger";
            } else { 
                type = "ORDER_PARTIAL"; 
                message = "주문 #" + o.getId() + " 품목이 부분 처리(일부 승인/반려) 되었습니다.";
                severity = "warning";
            }

            activities.add(DashboardEmpResponse.ActivityItem.builder()
                    .type(type)
                    .message(message)
                    .time(o.getProcessDate() != null ? o.getProcessDate().format(FEED_FMT) : "-")
                    .severity(severity)
                    .build());
        });

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        usageRepository.findByUser_IdAndProcessDateAfterOrderByProcessDateDesc(userId, startOfDay, PageRequest.of(0, 3))
        .forEach(u -> {
            activities.add(DashboardEmpResponse.ActivityItem.builder()
                    .type("STOCK_USED")
                    .message("매장 내 '" + u.getItem().getName() + "' " + u.getUsageQuantity() + "개를 사용 처리했습니다.")
                    .time(u.getProcessDate() != null ? u.getProcessDate().format(FEED_FMT) : "-")
                    .severity("warning")
                    .build());
        });

        return activities.stream()
                .sorted((a, b) -> b.getTime().compareTo(a.getTime()))
                .limit(10)
                .collect(Collectors.toList());
    }
}