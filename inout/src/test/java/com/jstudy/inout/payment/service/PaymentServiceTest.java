package com.jstudy.inout.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.dto.PaymentDto;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock private OrderRequestRepository orderRequestRepository;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long AMOUNT = 50_000L;

    private User requestUser;
    private OrderRequest order;
    private PaymentDto.Request request;

    @BeforeEach
    void setUp() {
        requestUser = User.builder()
                .id(USER_ID)
                .email("user@test.com")
                .password("encoded-pw")
                .name("테스트유저")
                .phone("010-0000-0000")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        order = OrderRequest.builder()
                .id(ORDER_ID)
                .requestUser(requestUser)
                .status(OrderStatus.REQUESTED)
                .totalPrice(AMOUNT)
                .build();

        request = PaymentDto.Request.builder()
                .orderId(ORDER_ID)
                .amount(AMOUNT)
                .build();
    }

    @Test
    @DisplayName("직원 직접 결제 차단 - REQUESTED 기안은 OWNER_APPROVAL_REQUIRED")
    void processDepositPayment_blocksEmployeeSelfPay() {
        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .satisfies(e -> {
                    InoutException ie = (InoutException) e;
                    assertThat(ie.getResultCode()).isEqualTo("OWNER_APPROVAL_REQUIRED");
                    assertThat(ie.getErrorCode()).isEqualTo(403);
                });

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
    }

    @Test
    @DisplayName("결제 실패 - 주문이 없으면 ORDER_NOT_FOUND")
    void processDepositPayment_Fail_OrderNotFound() {
        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .satisfies(e -> assertThat(((InoutException) e).getResultCode()).isEqualTo("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("결제 실패 - 타인 주문이면 FORBIDDEN")
    void processDepositPayment_Fail_Forbidden() {
        User other = User.builder().id(999L).email("other@test.com").password("x")
                .name("타유저").phone("010").birthday(LocalDate.of(1991, 1, 1)).build();
        OrderRequest otherOrder = OrderRequest.builder()
                .id(ORDER_ID).requestUser(other).status(OrderStatus.REQUESTED).totalPrice(AMOUNT).build();
        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .satisfies(e -> assertThat(((InoutException) e).getResultCode()).isEqualTo("FORBIDDEN"));
    }

    @Test
    @DisplayName("결제 실패 - REQUESTED가 아니면 INVALID_ORDER_STATUS")
    void processDepositPayment_Fail_InvalidOrderStatus() {
        OrderRequest ordered = OrderRequest.builder()
                .id(ORDER_ID).requestUser(requestUser).status(OrderStatus.ORDERED).totalPrice(AMOUNT).build();
        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(ordered));

        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .satisfies(e -> assertThat(((InoutException) e).getResultCode()).isEqualTo("INVALID_ORDER_STATUS"));
    }
}
