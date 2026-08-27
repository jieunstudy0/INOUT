package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 기동 시 필수 Role 행을 role 테이블에 자동으로 삽입한다.
 *
 * - 모든 프로필(local / dev / prod)에서 실행된다.
 * - 이미 존재하는 행은 SKIP(INSERT IGNORE 또는 NOT EXISTS)하므로 멱등성이 보장된다.
 * - Order(5) : DummyDataInitializer(Order 기본값 = Integer.MAX_VALUE)보다 먼저 실행된다.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class RoleInitRunner implements ApplicationRunner {

    private static final List<String> REQUIRED_ROLES = List.of(
            "ROLE_ADMIN",
            "ROLE_OWNER",
            "ROLE_EMPLOYEE",
            "ROLE_GUEST"   // 소셜 최초 가입 시 온보딩 완료 전 임시 권한
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (String roleName : REQUIRED_ROLES) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM role WHERE role_name = ?",
                        Integer.class, roleName);
                if (count == null || count == 0) {
                    jdbcTemplate.update("INSERT INTO role (role_name) VALUES (?)", roleName);
                    log.info("[RoleInitRunner] {} 행 생성 완료", roleName);
                    created++;
                }
            } catch (Exception e) {
                log.error("[RoleInitRunner] {} 초기화 실패: {}", roleName, e.getMessage(), e);
            }
        }
        if (created == 0) {
            log.debug("[RoleInitRunner] 모든 필수 Role이 이미 존재합니다. SKIP.");
        } else {
            log.info("[RoleInitRunner] 필수 Role {} 건 생성 완료", created);
        }
    }
}
