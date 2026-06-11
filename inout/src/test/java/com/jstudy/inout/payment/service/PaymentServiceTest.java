package com.jstudy.inout.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.dto.PaymentDto;
import com.jstudy.inout.payment.entity.DepositAccount;
import com.jstudy.inout.payment.entity.DepositHistory;
import com.jstudy.inout.payment.entity.TransactionType;
import com.jstudy.inout.payment.repository.DepositAccountRepository;
import com.jstudy.inout.payment.repository.DepositHistoryRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock private DepositAccountRepository accountRepository;
    @Mock private DepositHistoryRepository historyRepository;
    @Mock private OrderRequestRepository   orderRequestRepository;

    private static final Long USER_ID  = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long AMOUNT   = 50_000L;  
    private static final Long BALANCE  = 100_000L;
    private User             requestUser;
    private OrderRequest     order;
    private DepositAccount   account;
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

        account = DepositAccount.builder()
                .balance(BALANCE)
                .build();

        request = PaymentDto.Request.builder()
                .orderId(ORDER_ID)
                .amount(AMOUNT)
                .build();
    }

    @Test
    @DisplayName("결제성공_정상적인_예치금_결제시_주문상태가_PAID로_변경되고_History가_저장된다")
    void processDepositPayment_Success() {
        // Given
        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(accountRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(account));

        // When
        PaymentDto.Response response = paymentService.processDepositPayment(USER_ID, request);

        // Then ① 주문 상태 검증
        assertThat(order.getStatus())
                .as("결제 완료 후 주문 상태는 PAID여야 한다")
                .isEqualTo(OrderStatus.PAID);
        assertThat(order.getProcessDate())
                .as("결제 완료 후 처리 일시가 기록되어야 한다")
                .isNotNull();

        // Then ② 예치금 잔액 차감 검증 (100,000 - 50,000 = 50,000)
        assertThat(account.getBalance())
                .as("결제 금액만큼 예치금이 차감되어야 한다")
                .isEqualTo(BALANCE - AMOUNT);

        // Then ③ DepositHistory 저장 내용 검증 
        ArgumentCaptor<DepositHistory> historyCaptor = ArgumentCaptor.forClass(DepositHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        DepositHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getType())
                .as("결제 이력 유형은 PAYMENT여야 한다")
                .isEqualTo(TransactionType.PAYMENT);
        assertThat(savedHistory.getAmount())
                .as("이력에 기록된 금액이 결제 금액과 일치해야 한다")
                .isEqualTo(AMOUNT);
        assertThat(savedHistory.getRelatedOrderId())
                .as("이력에 연관 주문 ID가 기록되어야 한다")
                .isEqualTo(ORDER_ID);
        assertThat(savedHistory.getProcessedBy())
                .as("이력에 처리자 userId가 기록되어야 한다")
                .isEqualTo(USER_ID);
        assertThat(savedHistory.getDescription())
                .as("이력 설명에 주문번호가 포함되어야 한다")
                .contains(String.valueOf(ORDER_ID));

        // Then ④ 응답 DTO 검증
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getPaidAmount()).isEqualTo(AMOUNT);
        assertThat(response.getRemainingBalance())
                .as("응답의 잔여 잔액이 차감 후 값과 일치해야 한다")
                .isEqualTo(BALANCE - AMOUNT);
        assertThat(response.getMessage()).isEqualTo("예치금 결제가 성공적으로 완료되었습니다.");
    }

    @Test
    @DisplayName("결제실패_존재하지_않는_주문번호_조회시_ORDER_NOT_FOUND_예외가_발생한다")
    void processDepositPayment_Fail_OrderNotFound() {
        // Given
        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("존재하지 않는 주문입니다.")
                .satisfies(e -> {
                    InoutException ie = (InoutException) e;
                    assertThat(ie.getResultCode())
                            .as("에러 코드가 ORDER_NOT_FOUND여야 한다")
                            .isEqualTo("ORDER_NOT_FOUND");
                    assertThat(ie.getErrorCode())
                            .as("HTTP 상태 코드가 404여야 한다")
                            .isEqualTo(404);
                });


        verify(accountRepository, never()).findByUserIdForUpdate(any());
        verify(historyRepository,  never()).save(any());
    }

    @Test
    @DisplayName("결제실패_본인_주문이_아닌_경우_FORBIDDEN_예외가_발생한다")
    void processDepositPayment_Fail_Forbidden() {
        // Given
        User otherUser = User.builder()
                .id(99L)
                .email("other@test.com")
                .password("encoded-pw")
                .name("타인유저")
                .phone("010-9999-0000")
                .birthday(LocalDate.of(1995, 5, 5))
                .build();

        OrderRequest otherOrder = OrderRequest.builder()
                .id(ORDER_ID)
                .requestUser(otherUser)
                .status(OrderStatus.REQUESTED)
                .totalPrice(AMOUNT)
                .build();

        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(otherOrder));

        // When & Then
        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("본인의 주문만 결제할 수 있습니다.")
                .satisfies(e -> {
                    InoutException ie = (InoutException) e;
                    assertThat(ie.getResultCode()).isEqualTo("FORBIDDEN");
                    assertThat(ie.getErrorCode()).isEqualTo(403);
                });

        verify(accountRepository, never()).findByUserIdForUpdate(any());
        verify(historyRepository,  never()).save(any());
    }

    @Test
    @DisplayName("결제실패_주문_상태가_REQUESTED가_아닌_경우_INVALID_ORDER_STATUS_예외가_발생한다")
    void processDepositPayment_Fail_InvalidOrderStatus() {
        // Given
        OrderRequest paidOrder = OrderRequest.builder()
                .id(ORDER_ID)
                .requestUser(requestUser)
                .status(OrderStatus.PAID) 
                .totalPrice(AMOUNT)
                .build();

        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(paidOrder));

        // When & Then
        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("결제 대기 상태의 주문이 아닙니다.")
                .satisfies(e -> {
                    InoutException ie = (InoutException) e;
                    assertThat(ie.getResultCode()).isEqualTo("INVALID_ORDER_STATUS");
                    assertThat(ie.getErrorCode()).isEqualTo(400);
                    assertThat(ie.getMessage()).contains(OrderStatus.PAID.name());
                });

        verify(accountRepository, never()).findByUserIdForUpdate(any());
        verify(historyRepository,  never()).save(any());
    }

    @Test
    @DisplayName("결제실패_요청_결제금액이_실제_주문금액과_다른_경우_AMOUNT_MISMATCH_예외가_발생한다")
    void processDepositPayment_Fail_AmountMismatch() {
        // Given
        PaymentDto.Request tamperedRequest = PaymentDto.Request.builder()
                .orderId(ORDER_ID)
                .amount(99_999L)
                .build();

        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, tamperedRequest))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("요청하신 결제 금액이 실제 주문 금액과 일치하지 않습니다.")
                .satisfies(e -> {
                    InoutException ie = (InoutException) e;
                    assertThat(ie.getResultCode()).isEqualTo("AMOUNT_MISMATCH");
                    assertThat(ie.getErrorCode()).isEqualTo(400);
                });

        verify(accountRepository, never()).findByUserIdForUpdate(any());
        verify(historyRepository,  never()).save(any());
    }

    @Test
    @DisplayName("결제실패_요청_결제금액이_null인_경우_AMOUNT_MISMATCH_예외가_발생한다")
    void processDepositPayment_Fail_AmountNull() {
        // Given
        PaymentDto.Request nullAmountRequest = PaymentDto.Request.builder()
                .orderId(ORDER_ID)
                .amount(null)
                .build();

        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, nullAmountRequest))
                .isInstanceOf(InoutException.class)
                .satisfies(e -> assertThat(((InoutException) e).getResultCode())
                        .isEqualTo("AMOUNT_MISMATCH"));

        verify(accountRepository, never()).findByUserIdForUpdate(any());
        verify(historyRepository,  never()).save(any());
    }

    @Test
    @DisplayName("결제실패_예치금_계좌가_없는_경우_ACCOUNT_NOT_FOUND_예외가_발생한다")
    void processDepositPayment_Fail_AccountNotFound() {
        // Given
        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(accountRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(InoutException.class)
                .hasMessageContaining("예치금 계좌를 찾을 수 없습니다.")
                .satisfies(e -> {
                    InoutException ie = (InoutException) e;
                    assertThat(ie.getResultCode()).isEqualTo("ACCOUNT_NOT_FOUND");
                    assertThat(ie.getErrorCode()).isEqualTo(404);
                });

        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제실패_예치금_잔액이_부족한_경우_deductBalance에서_IllegalStateException이_발생한다")
    void processDepositPayment_Fail_InsufficientBalance() {
        // Given: 계좌 잔액(10,000원) < 결제 금액(50,000원)
        DepositAccount lowBalanceAccount = DepositAccount.builder()
                .balance(10_000L)
                .build();

        given(orderRequestRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
        given(accountRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(lowBalanceAccount));

        // When & Then
        assertThatThrownBy(() -> paymentService.processDepositPayment(USER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액이 부족합니다.");

        verify(historyRepository, never()).save(any());
        assertThat(order.getStatus())
                .as("결제 실패 시 주문 상태가 REQUESTED 그대로여야 한다")
                .isEqualTo(OrderStatus.REQUESTED);
        assertThat(lowBalanceAccount.getBalance())
                .as("결제 실패 시 잔액이 변경되지 않아야 한다")
                .isEqualTo(10_000L);
    }
}
