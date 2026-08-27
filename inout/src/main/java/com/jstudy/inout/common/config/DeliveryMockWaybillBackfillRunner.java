package com.jstudy.inout.common.config;

import com.jstudy.inout.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 앱 기동 시 기존 배송중/배송완료 데이터의 구형 송장을 Mock CJ 형식(56xxxxxxxxxx)으로 정규화.
 * DummyDataInitializer 이후 실행되도록 Order를 낮춘다.
 */
@Slf4j
@Component
@Order(100)
@Profile({"local", "demo", "dev"})
@RequiredArgsConstructor
public class DeliveryMockWaybillBackfillRunner implements ApplicationRunner {

    private final DeliveryService deliveryService;

    @Override
    public void run(ApplicationArguments args) {
        int updated = deliveryService.backfillMockWaybills();
        if (updated == 0) {
            log.info("[운송장 Mock 백필] 갱신 대상 없음 (이미 정규화됨)");
        }
    }
}
