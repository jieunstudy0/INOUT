package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 기존 RESIGNED 직원의 store_id 분리 (지점 통계·소속에서 제외).
 */
@Slf4j
@Component
@Order(55)
@RequiredArgsConstructor
public class ResignedStoreDetachRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE `user` SET `store_id` = NULL, `deleted` = 1 WHERE `status` = 'RESIGNED' AND (`store_id` IS NOT NULL OR `deleted` = 0)");
            if (updated > 0) {
                log.info("[HR] RESIGNED 직원 매장 분리 Soft-delete 백필 {}건", updated);
            }
        } catch (Exception e) {
            log.warn("[HR] RESIGNED 매장 분리 백필 스킵: {}", e.getMessage());
        }
    }
}
