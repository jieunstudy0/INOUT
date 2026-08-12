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
        User targetUser = getUser(targetUserId);
        DepositAccount account = getOrCreateAccountForUpdate(targetUser);

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
        User targetUser = getUser(targetUserId);
        DepositAccount account = getOrCreateAccountForUpdate(targetUser);

        account.addBalance(request.getAmount());
        saveHistory(account, TransactionType.REFUND, request.getAmount(), request.getDescription(), processedBy);

        return DepositDto.Response.builder()
                .userId(targetUserId)
                .currentBalance(account.getBalance())
                .message("환불 처리가 완료되었습니다.")
                .build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자 정보를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));
    }

    private DepositAccount getOrCreateAccountForUpdate(User user) {
        if (user.getStore() == null) {
            throw new InoutException("소속 매장이 없는 사용자는 예치금을 사용할 수 없습니다.", 403, "STORE_REQUIRED");
        }

        return accountRepository.findByStoreIdForUpdate(user.getStore().getId())
                .orElseGet(() -> {
                    DepositAccount newAccount = DepositAccount.builder()
                            .user(user)
                            .store(user.getStore())
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
                .balanceAfter(account.getBalance())
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
        deductDeposit(targetUserId, adminId, amount, description, null);
    }

    @Transactional
    public void deductDeposit(Long targetUserId, Long adminId, long amount, String description, Long orderId) {
        User targetUser = getUser(targetUserId);
        if (targetUser.getStore() == null) {
            throw new InoutException("소속 매장이 없는 사용자는 예치금을 사용할 수 없습니다.", 403, "STORE_REQUIRED");
        }

        DepositAccount account = accountRepository.findByStoreIdForUpdate(targetUser.getStore().getId())
                .orElseThrow(() -> new InoutException("예치금 계좌를 찾을 수 없습니다.", 404, "ACCOUNT_NOT_FOUND"));

        try {
            account.deductBalance(amount);
        } catch (IllegalStateException e) {
            throw new InoutException("예치금 잔액이 부족합니다.", 400, "INSUFFICIENT_BALANCE");
        }

        DepositHistory history = DepositHistory.builder()
                .depositAccount(account)
                .type(TransactionType.PAYMENT) 
                .amount(amount)
                .description(description)
                .relatedOrderId(orderId)
                .processedBy(adminId)
                .balanceAfter(account.getBalance())
                .build();
        historyRepository.save(history);
    }
    
}