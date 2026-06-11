package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "demo", "secret"})
@RequiredArgsConstructor
public class DummyDataScheduler {

    private final DummyDataService dummyDataService;

    @Scheduled(cron = "0 0 4 * * *")
    public void resetDummyDataEveryMorning() {
        log.info("⏰ 매일 새벽 4시 정기 작업: 데모 데이터 리셋을 시작합니다.");
        try {
            dummyDataService.clearAllData();      
            dummyDataService.generateDummyData(); 
            log.info("✅ 정기 데이터 리셋이 완료되었습니다.");
        } catch (Exception e) {
            log.error("❌ 정기 데이터 리셋 중 오류 발생", e);
        }
    }
}