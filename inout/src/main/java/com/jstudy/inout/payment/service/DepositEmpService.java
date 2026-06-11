package com.jstudy.inout.payment.service;

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

    public DepositEmpDto.HistoryResponse getMyDepositHistory(Long userId, Pageable pageable) {
        DepositAccount account = accountRepository.findByUserId(userId)
                .orElse(null);

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