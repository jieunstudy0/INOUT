package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "demo", "secret"})
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

    private final DummyDataService dummyDataService;

    @Override
    public void run(String... args) {
        log.info("[강제 초기화 모드] 기존 데이터를 모두 날리고 새로 세팅합니다!");

        dummyDataService.clearAllData();

        dummyDataService.generateDummyData();
    }
}