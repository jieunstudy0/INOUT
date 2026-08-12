package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * UserStatus 컬럼을 VARCHAR로 확장하고 구 LEAVE → ON_LEAVE 마이그레이션.
 * MySQL ENUM에 ON_LEAVE가 없으면 Data truncated 가 나므로 ALTER를 먼저 수행한다.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class UserStatusLeaveRenameRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE `user` MODIFY COLUMN `status` VARCHAR(20) NOT NULL");
            log.info("[UserStatus] status 컬럼을 VARCHAR(20)으로 확장했습니다.");
        } catch (Exception e) {
            log.warn("[UserStatus] status 컬럼 ALTER 스킵: {}", e.getMessage());
        }

        try {
            int updated = jdbcTemplate.update(
                    "UPDATE `user` SET `status` = 'ON_LEAVE' WHERE `status` = 'LEAVE'");
            if (updated > 0) {
                log.info("[UserStatus] LEAVE → ON_LEAVE 마이그레이션 {}건 완료", updated);
            }
        } catch (Exception e) {
            log.warn("[UserStatus] LEAVE 마이그레이션 스킵: {}", e.getMessage());
        }
    }
}
