package com.jstudy.inout.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Redis 접속 실패 등 캐시 연산 예외를 삼켜 캐시 미사용(Graceful Degradation)으로 전환한다.
 * {@link LoggingCacheErrorHandler}와 이중으로 보호하여, 어노테이션 AOP 경로 밖에서의
 * 직접 캐시 호출이나 핸들러 미등록 상황에서도 API가 500으로 실패하지 않게 한다.
 */
@Slf4j
public class ResilientCacheManager implements CacheManager {

    private final CacheManager delegate;

    public ResilientCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    @Nullable
    public Cache getCache(String name) {
        try {
            Cache cache = delegate.getCache(name);
            return cache == null ? null : new ResilientCache(cache);
        } catch (RuntimeException ex) {
            log.warn("[Cache] getCache({}) 실패 — 캐시 비활성 처리. error={}",
                    name, LoggingCacheErrorHandler.rootCauseMessage(ex));
            return null;
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        try {
            return delegate.getCacheNames();
        } catch (RuntimeException ex) {
            log.warn("[Cache] getCacheNames 실패. error={}",
                    LoggingCacheErrorHandler.rootCauseMessage(ex));
            return java.util.List.of();
        }
    }

    @Slf4j
    static final class ResilientCache implements Cache {

        private final Cache delegate;

        ResilientCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        @Nullable
        public ValueWrapper get(Object key) {
            try {
                return delegate.get(key);
            } catch (RuntimeException ex) {
                log.warn("[Cache GET 실패] cache={}, key={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), key, LoggingCacheErrorHandler.rootCauseMessage(ex));
                return null;
            }
        }

        @Override
        @Nullable
        public <T> T get(Object key, @Nullable Class<T> type) {
            try {
                return delegate.get(key, type);
            } catch (RuntimeException ex) {
                log.warn("[Cache GET 실패] cache={}, key={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), key, LoggingCacheErrorHandler.rootCauseMessage(ex));
                return null;
            }
        }

        @Override
        @Nullable
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                return delegate.get(key, valueLoader);
            } catch (RuntimeException ex) {
                log.warn("[Cache GET 실패] cache={}, key={}, error={} — valueLoader 직접 실행",
                        getName(), key, LoggingCacheErrorHandler.rootCauseMessage(ex));
                try {
                    return valueLoader.call();
                } catch (Exception loaderEx) {
                    if (loaderEx instanceof RuntimeException runtimeEx) {
                        throw runtimeEx;
                    }
                    throw new ValueRetrievalException(key, valueLoader, loaderEx);
                }
            }
        }

        @Override
        public void put(Object key, @Nullable Object value) {
            try {
                delegate.put(key, value);
            } catch (RuntimeException ex) {
                log.warn("[Cache PUT 실패] cache={}, key={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), key, LoggingCacheErrorHandler.rootCauseMessage(ex));
            }
        }

        @Override
        @Nullable
        public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
            try {
                return delegate.putIfAbsent(key, value);
            } catch (RuntimeException ex) {
                log.warn("[Cache PUT 실패] cache={}, key={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), key, LoggingCacheErrorHandler.rootCauseMessage(ex));
                return null;
            }
        }

        @Override
        public void evict(Object key) {
            try {
                delegate.evict(key);
            } catch (RuntimeException ex) {
                log.warn("[Cache EVICT 실패] cache={}, key={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), key, LoggingCacheErrorHandler.rootCauseMessage(ex));
            }
        }

        @Override
        public boolean evictIfPresent(Object key) {
            try {
                return delegate.evictIfPresent(key);
            } catch (RuntimeException ex) {
                log.warn("[Cache EVICT 실패] cache={}, key={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), key, LoggingCacheErrorHandler.rootCauseMessage(ex));
                return false;
            }
        }

        @Override
        public void clear() {
            try {
                delegate.clear();
            } catch (RuntimeException ex) {
                log.warn("[Cache CLEAR 실패] cache={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), LoggingCacheErrorHandler.rootCauseMessage(ex));
            }
        }

        @Override
        public boolean invalidate() {
            try {
                return delegate.invalidate();
            } catch (RuntimeException ex) {
                log.warn("[Cache CLEAR 실패] cache={}, error={} (캐시 없이 서비스 로직 계속)",
                        getName(), LoggingCacheErrorHandler.rootCauseMessage(ex));
                return false;
            }
        }
    }
}
