package com.jstudy.inout.payment.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.payment.dto.ChargeDto;
import com.jstudy.inout.payment.dto.DepositDto;
import com.jstudy.inout.payment.entity.ChargeRequest;
import com.jstudy.inout.payment.entity.ChargeStatus;
import com.jstudy.inout.payment.repository.ChargeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChargeService {

    private final ChargeRequestRepository chargeRequestRepository;
    private final UserRepository userRepository;
    private final DepositService depositService;

    @Transactional
    public void requestCharge(Long userId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new InoutException("충전 금액은 0보다 커야 합니다.", 400, "INVALID_AMOUNT");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        ChargeRequest request = ChargeRequest.builder()
                .requestUser(user)
                .amount(amount)
                .build();
        
        chargeRequestRepository.save(request);
    }

    @Transactional
    public void approveCharge(Long adminId, Long chargeRequestId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자를 찾을 수 없습니다.", 404, "ADMIN_NOT_FOUND"));

        ChargeRequest request = chargeRequestRepository.findById(chargeRequestId)
                .orElseThrow(() -> new InoutException("충전 요청을 찾을 수 없습니다.", 404, "REQUEST_NOT_FOUND"));

        if (request.getStatus() != ChargeStatus.PENDING) {
            throw new InoutException("이미 처리된 요청입니다.", 400, "ALREADY_PROCESSED");
        }

        request.approve(admin);

        DepositDto.ChargeRequest depositRequest = DepositDto.ChargeRequest.builder()
                .amount(request.getAmount())
                .description("예치금 충전 요청 승인 (요청번호: " + request.getId() + ")")
                .build();
                
        depositService.chargeDeposit(request.getRequestUser().getId(), adminId, depositRequest);
    }

    @Transactional
    public void rejectCharge(Long adminId, Long chargeRequestId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new InoutException("관리자를 찾을 수 없습니다.", 404, "ADMIN_NOT_FOUND"));

        ChargeRequest request = chargeRequestRepository.findById(chargeRequestId)
                .orElseThrow(() -> new InoutException("충전 요청을 찾을 수 없습니다.", 404, "REQUEST_NOT_FOUND"));

        if (request.getStatus() != ChargeStatus.PENDING) {
            throw new InoutException("이미 처리된 요청입니다.", 400, "ALREADY_PROCESSED");
        }

        request.reject(admin, reason);
    }

    @Transactional(readOnly = true)
    public List<ChargeDto.Response> getMyChargeRequests(Long userId) {
        return chargeRequestRepository.findAllByRequestUser_IdOrderByRequestDateDesc(userId)
                .stream()
                .map(ChargeDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChargeDto.Response> getPendingChargeRequests() {
        return chargeRequestRepository.findAllByStatusOrderByRequestDateDesc(ChargeStatus.PENDING)
                .stream()
                .map(ChargeDto.Response::from)
                .toList();
    }  
}