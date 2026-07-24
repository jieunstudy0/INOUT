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

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

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
