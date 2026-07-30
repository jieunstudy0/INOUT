package com.jstudy.inout.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig implements CachingConfigurer {

    public static final String DASHBOARD_SUMMARY = "dashboardSummary";

    public static final String STOCK_ALERTS      = "stockAlerts";

    public static final String STOCK_LIST        = "stockList";

    public static final String STORE_LIST        = "storeList";

    public static final String MAIL_TEMPLATE     = "mailTemplate";
    
    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisSerializer<Object> redisJsonSerializer;

    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaultConfig = buildDefaultConfig(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(DASHBOARD_SUMMARY, buildDefaultConfig(Duration.ofMinutes(5)));
        cacheConfigs.put(STOCK_ALERTS,      buildDefaultConfig(Duration.ofMinutes(3)));
        cacheConfigs.put(STOCK_LIST,        buildDefaultConfig(Duration.ofMinutes(3)));
        cacheConfigs.put(STORE_LIST,        buildDefaultConfig(Duration.ofHours(1)));
        cacheConfigs.put(MAIL_TEMPLATE,     buildDefaultConfig(Duration.ofHours(1)));

        CacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();

        // Redis 접속 불가 시에도 get/put/evict가 예외를 던지지 않도록 래핑
        return new ResilientCacheManager(redisCacheManager);
    }

    /**
     * Redis 미기동·타임아웃 등 캐시 장애를 API 500으로 전파하지 않는다.
     * {@link CachingConfigurer}가 캐시 어드바이저에 이 핸들러를 연결한다.
     * (이 메서드에는 {@code @Bean}을 붙이지 않는다 — 이중 등록으로 충돌할 수 있음)
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private RedisCacheConfiguration buildDefaultConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(redisJsonSerializer))
                .disableCachingNullValues();
    }
}
