package com.jstudy.inout.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

class ResilientCacheGracefulDegradationTest {

    @Test
    @DisplayName("ResilientCacheManager: Redis 연결 실패를 시뮬레이션해도 get/put/evict가 예외를 던지지 않는다")
    void resilientCache_swallowsRedisConnectionFailures() {
        CacheManager manager = new ResilientCacheManager(new AlwaysFailingCacheManager());
        Cache cache = manager.getCache("dashboardSummary");

        assertThat(cache).isNotNull();
        assertThatCode(() -> cache.get("admin")).doesNotThrowAnyException();
        assertThat(cache.get("admin")).isNull();
        assertThatCode(() -> cache.put("admin", "payload")).doesNotThrowAnyException();
        assertThatCode(() -> cache.evict("admin")).doesNotThrowAnyException();
        assertThatCode(cache::clear).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("@Cacheable + LoggingCacheErrorHandler: Redis PUT/GET 실패 시에도 서비스 결과가 정상 반환된다")
    void cacheable_withErrorHandler_returnsServiceResultWhenRedisDown() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(FailingRedisCacheTestConfig.class)) {
            CachedProbeService service = ctx.getBean(CachedProbeService.class);

            String first = service.loadSummary();
            String second = service.loadSummary();

            assertThat(first).isEqualTo("db-result");
            assertThat(second).isEqualTo("db-result");
            assertThat(service.invocationCount()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Unable to connect to Redis 메시지가 있어도 CacheErrorHandler가 예외를 전파하지 않는다")
    void loggingHandler_swallowsUnableToConnectMessage() {
        LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler();
        Cache cache = new ConcurrentMapCache("dashboardSummary");
        RuntimeException redisDown = new RuntimeException(
                "Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException");

        assertThatCode(() -> handler.handleCacheGetError(redisDown, cache, "admin"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(redisDown, cache, "admin", "v"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(redisDown, cache, "admin"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(redisDown, cache))
                .doesNotThrowAnyException();
        assertThat(LoggingCacheErrorHandler.rootCauseMessage(redisDown))
                .contains("Unable to connect to Redis");
    }

    static final class AlwaysFailingCacheManager implements CacheManager {
        @Override
        public Cache getCache(String name) {
            return new AlwaysFailingCache(name);
        }

        @Override
        public Collection<String> getCacheNames() {
            return List.of("dashboardSummary");
        }
    }

    static final class AlwaysFailingCache implements Cache {
        private final String name;

        AlwaysFailingCache(String name) {
            this.name = name;
        }

        private RuntimeException fail() {
            return new RuntimeException("Unable to connect to Redis");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        public ValueWrapper get(Object key) {
            throw fail();
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            throw fail();
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            throw fail();
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            throw fail();
        }

        @Override
        public void evict(Object key) {
            throw fail();
        }

        @Override
        public void clear() {
            throw fail();
        }
    }

    @Configuration
    @EnableCaching
    static class FailingRedisCacheTestConfig implements CachingConfigurer {

        @Bean
        @Override
        public CacheManager cacheManager() {
            return new ResilientCacheManager(new AlwaysFailingCacheManager());
        }

        @Bean
        @Override
        public CacheErrorHandler errorHandler() {
            return new LoggingCacheErrorHandler();
        }

        @Bean
        CachedProbeService cachedProbeService() {
            return new CachedProbeService();
        }
    }

    @Service
    static class CachedProbeService {
        private final AtomicInteger invocations = new AtomicInteger();

        @Cacheable(value = "dashboardSummary", key = "'admin'")
        public String loadSummary() {
            invocations.incrementAndGet();
            return "db-result";
        }

        public int invocationCount() {
            return invocations.get();
        }
    }
}
