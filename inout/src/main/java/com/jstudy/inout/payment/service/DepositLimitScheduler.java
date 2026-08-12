package com.jstudy.inout.payment.service;

import com.jstudy.inout.common.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매일 자정 직원별 todayUsedDeposit 초기화.
 * deposit.scheduler.enabled=true 일 때만 활성.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "deposit.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class DepositLimitScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    @Transactional
    public void resetTodayUsedDeposit() {
        log.info("[예치금 일일 한도] todayUsedDeposit 자정 초기화 시작");
        int updated = userRepository.resetAllTodayUsedDeposit();
        log.info("[예치금 일일 한도] todayUsedDeposit 초기화 완료 — {}건", updated);
    }
}
