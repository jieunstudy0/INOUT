package com.jstudy.inout.common.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

@Slf4j
@Configuration
@Profile("dev")
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = RedisServer.newRedisServer()
                    .port(redisPort)
                    .setting("bind 127.0.0.1")
                    .setting("maxmemory 128mb")
                    .build();
            redisServer.start();
            log.info("[내장형 Redis] 로컬 개발용 Embedded Redis 서버를 포트 {}에서 시작했습니다.", redisPort);
        } catch (Exception e) {
            log.warn("[내장형 Redis] Embedded Redis 서버 시작에 실패했습니다 (포트 {}). "
                            + "이미 같은 포트에서 Redis가 실행 중이라면 정상 동작에는 문제가 없습니다. 원인: {}",
                    redisPort, e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            try {
                redisServer.stop();
                log.info("[내장형 Redis] Embedded Redis 서버를 정상 종료했습니다.");
            } catch (Exception e) {
                log.warn("[내장형 Redis] Embedded Redis 서버 종료 중 오류가 발생했습니다. 원인: {}", e.getMessage());
            }
        }
    }
}
