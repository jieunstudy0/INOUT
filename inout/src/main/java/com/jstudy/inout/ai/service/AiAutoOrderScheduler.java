package com.jstudy.inout.ai.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jstudy.inout.common.exception.InoutException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiAutoOrderScheduler {

    private final AiAutoOrderService aiAutoOrderService;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void run() {
        log.info("[AI 자동 발주 스케줄러] 실행 시작");
        try {
            aiAutoOrderService.createAutoOrderDraft(); 
        } catch (InoutException e) {
            log.warn("[AI 자동 발주 스케줄러] 처리 중단: {} ({})",
                    e.getMessage(), e.getResultCode());
        } catch (Exception e) {
            log.error("[AI 자동 발주 스케줄러] 예상치 못한 오류 발생", e);
        }
    }
}
