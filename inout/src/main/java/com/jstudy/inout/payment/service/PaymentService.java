package com.jstudy.inout.payment.service;

import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.payment.dto.PaymentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 레거시 직원 직접 결제 API.
 * 프랜차이즈 3단계에서는 점주 modify-and-approve 만 예치금을 차감한다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRequestRepository orderRequestRepository;

    @Transactional
    public PaymentDto.Response processDepositPayment(Long userId, PaymentDto.Request request) {

        OrderRequest order = orderRequestRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new InoutException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getRequestUser().getId().equals(userId)) {
            throw new InoutException(ErrorCode.NOT_OWNER, "본인의 발주만 결제할 수 있습니다.");
        }

        if (order.getStatus() == OrderStatus.REQUESTED) {
            throw new InoutException(ErrorCode.OWNER_APPROVAL_REQUIRED);
        }

        throw new InoutException(ErrorCode.INVALID_ORDER_STATUS, "결제 대기 상태의 발주가 아닙니다.");
    }
}
