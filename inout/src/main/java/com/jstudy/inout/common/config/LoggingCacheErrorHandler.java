package com.jstudy.inout.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Redis 등 캐시 백엔드 장애 시 Spring Cache 어드바이저가 예외를 전파하지 않도록 한다.
 * GET 실패 → 캐시 미스로 간주 후 본래 서비스(DB/AI) 로직 수행,
 * PUT/EVICT/CLEAR 실패 → 로그만 남기고 서비스 결과는 그대로 반환한다.
 */
@Slf4j
public class LoggingCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        logCacheFailure("GET", cache, key, exception);
        // 절대 rethrow 하지 않음 → CacheAspectSupport가 null(miss)로 처리 후 메서드 본문 실행
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logCacheFailure("PUT", cache, key, exception);
        // 절대 rethrow 하지 않음 → 이미 계산된 서비스 결과를 API 응답으로 반환
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logCacheFailure("EVICT", cache, key, exception);
        // 절대 rethrow 하지 않음 → 비즈니스 트랜잭션/응답에 영향 없음
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logCacheFailure("CLEAR", cache, null, exception);
    }

    private void logCacheFailure(String operation, Cache cache, Object key, RuntimeException exception) {
        String cacheName = cache != null ? cache.getName() : "unknown";
        String root = rootCauseMessage(exception);
        if (key == null) {
            log.warn("[Cache {} 실패] cache={}, error={} (캐시 없이 서비스 로직 계속)",
                    operation, cacheName, root);
        } else {
            log.warn("[Cache {} 실패] cache={}, key={}, error={} (캐시 없이 서비스 로직 계속)",
                    operation, cacheName, key, root);
        }
        if (log.isDebugEnabled()) {
            log.debug("[Cache {} 실패] stacktrace", operation, exception);
        }
    }

    static String rootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }
        return message;
    }
}
