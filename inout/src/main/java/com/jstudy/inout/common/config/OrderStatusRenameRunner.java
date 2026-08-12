package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 발주 상태 컬럼 확장 + 3단계 모델 마이그레이션.
 * MySQL ENUM/짧은 VARCHAR에 ORDERED·APPROVED 등이 없으면 Data truncated(1265)가 나므로
 * VARCHAR(30) ALTER를 먼저 수행한 뒤 PAID→ORDERED, COMPLETED→APPROVED를 반영한다.
 */
@Slf4j
@Component
@Order(45)
@RequiredArgsConstructor
public class OrderStatusRenameRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        widenStatusColumn();
        migrateLegacyValues();
    }

    private void widenStatusColumn() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE `order_request` MODIFY COLUMN `status` VARCHAR(30) NOT NULL");
            log.info("[OrderStatus] order_request.status 컬럼을 VARCHAR(30)으로 확장했습니다.");
        } catch (Exception e) {
            log.warn("[OrderStatus] status 컬럼 ALTER 스킵: {}", e.getMessage());
        }
    }

    private void migrateLegacyValues() {
        try {
            int toOrdered = jdbcTemplate.update(
                    "UPDATE `order_request` SET `status` = 'ORDERED' WHERE `status` = 'PAID'");
            int toApproved = jdbcTemplate.update(
                    "UPDATE `order_request` SET `status` = 'APPROVED' WHERE `status` = 'COMPLETED'");
            if (toOrdered + toApproved > 0) {
                log.info("[OrderStatus] 마이그레이션 — PAID→ORDERED {}건, COMPLETED→APPROVED {}건",
                        toOrdered, toApproved);
            }
        } catch (Exception e) {
            log.warn("[OrderStatus] 값 마이그레이션 스킵: {}", e.getMessage());
        }
    }
}
