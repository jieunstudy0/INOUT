package com.jstudy.inout.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

@ExtendWith(MockitoExtension.class)
class LoggingCacheErrorHandlerTest {

    private LoggingCacheErrorHandler handler;
    private Cache mockCache;
    private RuntimeException simulatedRedisFailure;

    @BeforeEach
    void setUp() {
        handler = new LoggingCacheErrorHandler();
        mockCache = mock(Cache.class);
        simulatedRedisFailure = new RuntimeException("Redis 연결 실패 시뮬레이션");
    }

    @Test
    @DisplayName("handleCacheGetError: Redis 조회 실패 시 예외를 전파하지 않아 DB Fallback이 가능하다")
    void handleCacheGetError_doesNotPropagateException() {
        // given
        org.mockito.BDDMockito.given(mockCache.getName()).willReturn("dashboardSummary");

        // when & then - 예외가 전파되지 않아야 Fallback이 동작함
        assertThatCode(() ->
            handler.handleCacheGetError(simulatedRedisFailure, mockCache, "'admin'")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCachePutError: Redis 저장 실패 시 예외를 전파하지 않아 응답을 정상 반환한다")
    void handleCachePutError_doesNotPropagateException() {
        org.mockito.BDDMockito.given(mockCache.getName()).willReturn("stockAlerts");

        assertThatCode(() ->
            handler.handleCachePutError(simulatedRedisFailure, mockCache, "'lowStock'", "value")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCacheEvictError: Redis 삭제 실패 시 예외를 전파하지 않아 재고 변동 처리가 계속된다")
    void handleCacheEvictError_doesNotPropagateException() {
        org.mockito.BDDMockito.given(mockCache.getName()).willReturn("stockAlerts");

        assertThatCode(() ->
            handler.handleCacheEvictError(simulatedRedisFailure, mockCache, "'lowStock'")
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCacheClearError: Redis 전체 삭제 실패 시 예외를 전파하지 않는다")
    void handleCacheClearError_doesNotPropagateException() {
        org.mockito.BDDMockito.given(mockCache.getName()).willReturn("storeList");

        assertThatCode(() ->
            handler.handleCacheClearError(simulatedRedisFailure, mockCache)
        ).doesNotThrowAnyException();
    }
}
