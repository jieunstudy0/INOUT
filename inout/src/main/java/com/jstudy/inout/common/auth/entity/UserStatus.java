package com.jstudy.inout.common.auth.entity;

/**
 * 직원 계정 상태 (HR).
 * ACTIVE(재직)만 로그인 허용.
 */
public enum UserStatus {
    ACTIVE,    // 재직
    ON_LEAVE,  // 휴직 (구 LEAVE)
    RESIGNED;  // 퇴사

    /** 로그인 가능 여부 — ACTIVE만 허용 */
    public boolean allowsLogin() {
        return this == ACTIVE;
    }

    /**
     * API/필터 문자열 파싱. 구버전 {@code LEAVE}는 {@code ON_LEAVE}로 매핑.
     */
    public static UserStatus fromApi(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if ("LEAVE".equalsIgnoreCase(value)) {
            return ON_LEAVE;
        }
        return UserStatus.valueOf(value.toUpperCase());
    }
}
