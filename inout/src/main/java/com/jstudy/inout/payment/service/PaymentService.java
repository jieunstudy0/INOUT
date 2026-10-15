package com.jstudy.inout.payment.service;

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
                .orElseThrow(() -> new InoutException("발주 정보를 찾을 수 없습니다.", 404, "ORDER_NOT_FOUND"));

        if (!order.getRequestUser().getId().equals(userId)) {
            throw new InoutException("본인의 발주만 결제할 수 있습니다.", 403, "NOT_OWNER");
        }

        if (order.getStatus() == OrderStatus.REQUESTED) {
            throw new InoutException(
                    "직원 기안은 점주가 승인·결제한 뒤에만 본사로 전달됩니다. 예치금 결제는 점주 화면에서 진행해 주세요.",
                    403,
                    "OWNER_APPROVAL_REQUIRED");
        }

        throw new InoutException(
                "결제 대기 상태의 발주가 아닙니다.",
                400,
                "INVALID_ORDER_STATUS");
    }
}
