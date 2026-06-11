package com.jstudy.inout.payment.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.payment.dto.DepositDto;
import com.jstudy.inout.payment.entity.*;
import com.jstudy.inout.payment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepositService {

    private final DepositAccountRepository accountRepository;
    private final DepositHistoryRepository historyRepository;
    private final UserRepository userRepository; 


    @Transactional
    public DepositDto.Response chargeDeposit(Long targetUserId, Long processedBy, DepositDto.ChargeRequest request) {

        validateAmount(request.getAmount());
        DepositAccount account = getOrCreateAccountForUpdate(targetUserId);

        account.addBalance(request.getAmount());

        saveHistory(account, TransactionType.CHARGE, request.getAmount(), request.getDescription(), processedBy);

        return DepositDto.Response.builder()
                .userId(targetUserId)
                .currentBalance(account.getBalance())
                .message("충전이 완료되었습니다.")
                .build();
    }


    @Transactional
    public DepositDto.Response refundDeposit(Long targetUserId, Long processedBy, DepositDto.RefundRequest request) {

        validateAmount(request.getAmount());
        DepositAccount account = getOrCreateAccountForUpdate(targetUserId);

        account.addBalance(request.getAmount());
        saveHistory(account, TransactionType.REFUND, request.getAmount(), request.getDescription(), processedBy);

        return DepositDto.Response.builder()
                .userId(targetUserId)
                .currentBalance(account.getBalance())
                .message("환불 처리가 완료되었습니다.")
                .build();
    }

    private DepositAccount getOrCreateAccountForUpdate(Long userId) {
        return accountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
                    DepositAccount newAccount = DepositAccount.builder()
                            .user(user)
                            .balance(0L)
                            .build();
                    return accountRepository.save(newAccount);
                });
    }


    private void saveHistory(DepositAccount account, TransactionType type,
            Long amount, String desc, Long processedBy) {
        DepositHistory history = DepositHistory.builder()
                .depositAccount(account)
                .type(type)
                .amount(amount)
                .description(desc)
                .processedBy(processedBy) 
                .build();
        historyRepository.save(history);
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
    }
    
    @Transactional
    public void deductDeposit(Long targetUserId, Long adminId, long amount, String description) {
        DepositAccount account = accountRepository.findByUserIdForUpdate(targetUserId)
                .orElseThrow(() -> new InoutException("예치금 계좌를 찾을 수 없습니다.", 404, "ACCOUNT_NOT_FOUND"));

        account.deductBalance(amount);

        DepositHistory history = DepositHistory.builder()
                .depositAccount(account)
                .type(TransactionType.PAYMENT) 
                .amount(amount)
                .description(description)
                .processedBy(adminId)
                .build();
        historyRepository.save(history);
    }
    
}