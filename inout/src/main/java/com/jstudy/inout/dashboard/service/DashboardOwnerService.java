package com.jstudy.inout.dashboard.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.dashboard.dto.DashboardOwnerResponse;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.leave.entity.LeaveStatus;
import com.jstudy.inout.leave.repository.AnnualLeaveRepository;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.repository.DepositAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardOwnerService {

    private final UserRepository userRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final DeliveryRepository deliveryRepository;
    private final AnnualLeaveRepository annualLeaveRepository;
    private final DepositAccountRepository depositAccountRepository;

    public DashboardOwnerResponse getSummary(Long ownerUserId) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        if (owner.getStore() == null) {
            throw new InoutException("소속 매장 정보가 없습니다.", 403, "STORE_REQUIRED");
        }

        Long storeId = owner.getStore().getId();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long depositBalance = depositAccountRepository.findByStoreId(storeId)
                .map(account -> account.getBalance())
                .orElse(0L);

        return DashboardOwnerResponse.builder()
                .ownerName(owner.getName())
                .storeName(owner.getStore().getName())
                .storeId(storeId)
                .depositBalance(depositBalance)
                .todayOrderCount(orderRequestRepository.countTodayOrdersByStoreId(storeId, startOfDay))
                .pendingOrderCount(orderRequestRepository.countByStoreIdAndStatus(storeId, OrderStatus.PAID))
                .readyDeliveryCount(deliveryRepository.countByStoreIdAndStatus(storeId, DeliveryStatus.READY))
                .shippingDeliveryCount(deliveryRepository.countByStoreIdAndStatus(storeId, DeliveryStatus.SHIPPING))
                .completedDeliveryCount(deliveryRepository.countByStoreIdAndStatus(storeId, DeliveryStatus.COMPLETED))
                .pendingLeaveCount(annualLeaveRepository.countByStoreIdAndStatus(storeId, LeaveStatus.PENDING))
                .staffCount(userRepository.countByStore_Id(storeId))
                .lockedStaffCount(userRepository.countByStore_IdAndIsLockedTrue(storeId))
                .build();
    }
}
