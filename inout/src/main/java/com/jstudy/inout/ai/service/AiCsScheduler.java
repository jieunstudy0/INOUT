package com.jstudy.inout.ai.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jstudy.inout.common.exception.InoutException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ai.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class AiCsScheduler {

    private final AiCsService aiCsService;

    @Scheduled(cron = "0 */10 * * * ?", zone = "Asia/Seoul")
    public void run() {
        log.info("[AI CS 자동화 스케줄러] 실행 시작");
        try {
            int processed = aiCsService.processWaitingInquiries();
            log.info("[AI CS 자동화 스케줄러] {}건 처리 완료", processed);
        } catch (InoutException e) {
            log.warn("[AI CS 자동화 스케줄러] 처리 중단: {} ({})",
                    e.getMessage(), e.getResultCode());
        } catch (Exception e) {
            log.error("[AI CS 자동화 스케줄러] 예상치 못한 오류 발생", e);
        }
    }
}
