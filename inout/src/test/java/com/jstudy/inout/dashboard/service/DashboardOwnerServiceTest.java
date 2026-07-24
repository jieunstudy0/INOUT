package com.jstudy.inout.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.jstudy.inout.common.auth.entity.Store;
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
import com.jstudy.inout.payment.entity.DepositAccount;
import com.jstudy.inout.payment.repository.DepositAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardOwnerServiceTest {

    @InjectMocks
    private DashboardOwnerService dashboardOwnerService;

    @Mock private UserRepository userRepository;
    @Mock private OrderRequestRepository orderRequestRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private AnnualLeaveRepository annualLeaveRepository;
    @Mock private DepositAccountRepository depositAccountRepository;

    @Test
    @DisplayName("점주 대시보드 집계 성공 - 매장 KPI를 반환한다")
    void getSummary_success() {
        // given
        Store store = Store.builder().id(10L).name("지점 1호").address("서울").build();
        User owner = User.builder().id(5L).name("점주1").store(store).build();
        given(userRepository.findById(5L)).willReturn(Optional.of(owner));
        given(depositAccountRepository.findByStoreId(10L))
                .willReturn(Optional.of(DepositAccount.builder().store(store).balance(50_000_000L).build()));
        given(orderRequestRepository.countTodayOrdersByStoreId(eq(10L), any())).willReturn(4L);
        given(orderRequestRepository.countByStoreIdAndStatus(10L, OrderStatus.PAID)).willReturn(2L);
        given(deliveryRepository.countByStoreIdAndStatus(10L, DeliveryStatus.READY)).willReturn(1L);
        given(deliveryRepository.countByStoreIdAndStatus(10L, DeliveryStatus.SHIPPING)).willReturn(3L);
        given(deliveryRepository.countByStoreIdAndStatus(10L, DeliveryStatus.COMPLETED)).willReturn(5L);
        given(annualLeaveRepository.countByStoreIdAndStatus(10L, LeaveStatus.PENDING)).willReturn(2L);
        given(userRepository.countByStore_Id(10L)).willReturn(6L);
        given(userRepository.countByStore_IdAndIsLockedTrue(10L)).willReturn(1L);

        // when
        DashboardOwnerResponse response = dashboardOwnerService.getSummary(5L);

        // then
        assertThat(response.getStoreName()).isEqualTo("지점 1호");
        assertThat(response.getTodayOrderCount()).isEqualTo(4L);
        assertThat(response.getShippingDeliveryCount()).isEqualTo(3L);
        assertThat(response.getPendingLeaveCount()).isEqualTo(2L);
        assertThat(response.getDepositBalance()).isEqualTo(50_000_000L);
    }

    @Test
    @DisplayName("점주 대시보드 집계 실패 - 소속 매장이 없으면 STORE_REQUIRED")
    void getSummary_fail_storeRequired() {
        // given
        User owner = User.builder().id(5L).name("점주1").build();
        given(userRepository.findById(5L)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> dashboardOwnerService.getSummary(5L))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode")
                .isEqualTo("STORE_REQUIRED");
    }
}
