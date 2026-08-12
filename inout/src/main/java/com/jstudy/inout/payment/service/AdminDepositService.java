package com.jstudy.inout.payment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import com.jstudy.inout.payment.dto.AdminDepositDto;
import com.jstudy.inout.payment.repository.DepositHistoryRepository;
import com.jstudy.inout.payment.repository.DepositAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDepositService {

    private final DepositHistoryRepository depositHistoryRepository;
    private final DepositAccountRepository depositAccountRepository;

    public AdminDepositDto.ListResponse getAdminDepositHistory(Long storeId, String type, String keyword, Pageable pageable) {
        
        long totalBalance = depositAccountRepository.calculateTotalDepositBalance();        
        long monthlyCharge = depositHistoryRepository.calculateMonthlyCharge();
        long monthlyUsage = depositHistoryRepository.calculateMonthlyUsage();

        AdminDepositDto.Summary summary = AdminDepositDto.Summary.builder()
                .totalBalance(totalBalance)
                .monthlyCharge(monthlyCharge)
                .monthlyUsage(monthlyUsage)
                .build();

        Page<AdminDepositDto.HistoryItem> historyPage = depositHistoryRepository
                .findAdminHistoriesByFilters(storeId, type, keyword, pageable)
                .map(history -> AdminDepositDto.HistoryItem.builder()
                        .id(history.getId())                       
                        .storeName(history.getDepositAccount().getUser().getName())                         
                        .type(history.getType().name()) 
                        .amount(history.getAmount())
                        .balanceAfter(history.getDepositAccount().getBalance())                        
                        .description(history.getDescription()) 
                        .createdAt(history.getCreatedAt())
                        .build());

        return AdminDepositDto.ListResponse.builder()
                .summary(summary)
                .histories(historyPage)
                .build();
    }
    
    
    public List<AdminDepositDto.FranchiseeInfo> getFranchiseeList() {

        return depositAccountRepository.findAllWithUserAndStore().stream()
                .map(account -> {
               
                    String storeName = (account.getUser().getStore() != null) 
                            ? account.getUser().getStore().getName() 
                            : "본사/미지정";

                    return AdminDepositDto.FranchiseeInfo.builder()
                            .userId(account.getUser().getId())
                            .userName(account.getUser().getName())
                            .email(account.getUser().getEmail()) 
                            .storeName(storeName) 
                            .build();
                })
                .collect(Collectors.toList());
    }
    
}