package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * inquiries 테이블에 target_type 컬럼을 추가하고
 * 기존 데이터를 기본값 'ADMIN'으로 초기화하는 DB 마이그레이션 Runner.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class InquiryTargetTypeRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean columnExists = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) > 0 FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inquiries' AND COLUMN_NAME = 'target_type'",
                    Boolean.class));

            if (!columnExists) {
                log.info("[InquiryTargetTypeRunner] target_type 컬럼 추가 시작");
                jdbcTemplate.execute(
                        "ALTER TABLE inquiries ADD COLUMN target_type VARCHAR(10) NOT NULL DEFAULT 'ADMIN'");
                log.info("[InquiryTargetTypeRunner] target_type 컬럼 추가 완료 — 기존 데이터 기본값 'ADMIN' 적용됨");
            } else {
                log.debug("[InquiryTargetTypeRunner] target_type 컬럼이 이미 존재합니다. 스킵.");
            }
        } catch (Exception e) {
            log.error("[InquiryTargetTypeRunner] 마이그레이션 실패: {}", e.getMessage(), e);
        }
    }
}
