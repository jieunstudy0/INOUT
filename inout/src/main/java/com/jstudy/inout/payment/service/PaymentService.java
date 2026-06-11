package com.jstudy.inout.payment.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final DepositAccountRepository accountRepository;
    private final DepositHistoryRepository historyRepository;
    private final OrderRequestRepository orderRequestRepository; 

    @Transactional
    public PaymentDto.Response processDepositPayment(Long userId, PaymentDto.Request request) {

        OrderRequest order = orderRequestRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new InoutException("존재하지 않는 주문입니다.", 404, "ORDER_NOT_FOUND"));

        if (!order.getRequestUser().getId().equals(userId)) {
            throw new InoutException("본인의 주문만 결제할 수 있습니다.", 403, "FORBIDDEN");
        }

        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new InoutException("결제 대기 상태의 주문이 아닙니다. (현재 상태: " + order.getStatus() + ")", 400, "INVALID_ORDER_STATUS");
        }

        if (order.getTotalPrice() == null || request.getAmount() == null
                || !order.getTotalPrice().equals(request.getAmount())) {
            throw new InoutException("요청하신 결제 금액이 실제 주문 금액과 일치하지 않습니다.", 400, "AMOUNT_MISMATCH");
        }

        DepositAccount account = accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new InoutException("예치금 계좌를 찾을 수 없습니다.", 404, "ACCOUNT_NOT_FOUND"));

        account.deductBalance(request.getAmount());

        DepositHistory history = DepositHistory.builder()
                .depositAccount(account)
                .type(TransactionType.PAYMENT)
                .amount(request.getAmount())
                .description("발주 주문번호 [" + order.getId() + "] 대금 결제")
                .relatedOrderId(order.getId()) 
                .processedBy(userId)           
                .build();
        historyRepository.save(history);

        order.updateStatus(OrderStatus.PAID);
        order.updateProcessDate(LocalDateTime.now());

        return PaymentDto.Response.builder()
                .orderId(order.getId())
                .paidAmount(request.getAmount())
                .remainingBalance(account.getBalance())
                .message("예치금 결제가 성공적으로 완료되었습니다.")
                .build();
    }
}