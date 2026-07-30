package com.jstudy.inout.payment.service;

import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.payment.dto.DepositEmpDto;
import com.jstudy.inout.payment.entity.DepositAccount;
import com.jstudy.inout.payment.entity.DepositHistory;
import com.jstudy.inout.payment.repository.DepositAccountRepository;
import com.jstudy.inout.payment.repository.DepositHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepositEmpService {

    private final DepositAccountRepository accountRepository;
    private final DepositHistoryRepository historyRepository;
    private final UserRepository userRepository;

    /**
     * 직원 조회는 매장 지갑 기준 (결제와 동일).
     * 개인 계좌가 있으면 우선, 없으면 소속 매장 계좌로 폴백.
     */
    public DepositEmpDto.HistoryResponse getMyDepositHistory(Long userId, Pageable pageable) {
        DepositAccount account = accountRepository.findByUserId(userId).orElse(null);
        if (account == null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getStore() != null) {
                account = accountRepository.findByStoreId(user.getStore().getId()).orElse(null);
            }
        }
        return toHistoryResponse(account, pageable);
    }

    public DepositEmpDto.HistoryResponse getStoreDepositHistory(Long storeId, Pageable pageable) {
        DepositAccount account = accountRepository.findByStoreId(storeId)
                .orElse(null);
        return toHistoryResponse(account, pageable);
    }

    private DepositEmpDto.HistoryResponse toHistoryResponse(DepositAccount account, Pageable pageable) {
        if (account == null) {
            return DepositEmpDto.HistoryResponse.builder()
                    .currentBalance(0L)
                    .histories(Page.empty(pageable))
                    .build();
        }

        Page<DepositHistory> historyPage = historyRepository
                .findByDepositAccountIdOrderByCreatedAtDesc(account.getId(), pageable);

        Page<DepositEmpDto.HistoryItem> itemPage = historyPage.map(h ->
                DepositEmpDto.HistoryItem.builder()
                        .id(h.getId())
                        .type(h.getType())
                        .amount(h.getAmount())
                        .description(h.getDescription())
                        .createdAt(h.getCreatedAt())
                        .build()
        );

        return DepositEmpDto.HistoryResponse.builder()
                .currentBalance(account.getBalance())
                .histories(itemPage)
                .build();
    }
}
