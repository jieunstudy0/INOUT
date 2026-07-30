package com.jstudy.inout.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * ai.scheduler.enabled 가 true 가 아니면 스케줄러 빈이 등록되지 않아
 * Gemini 자동 호출이 발생하지 않음을 보증한다.
 */
class AiSchedulerConditionalOnPropertyTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AiSchedulerTestConfig.class);

    @Test
    @DisplayName("프로퍼티 미설정 시(matchIfMissing=false) 스케줄러 빈 미등록")
    void beansAbsentWhenPropertyMissing() {
        contextRunner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(AiCsScheduler.class);
            assertThat(ctx).doesNotHaveBean(AiAutoOrderScheduler.class);
        });
    }

    @Test
    @DisplayName("ai.scheduler.enabled=false 이면 스케줄러 빈 미등록")
    void beansAbsentWhenPropertyFalse() {
        contextRunner
                .withPropertyValues("ai.scheduler.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(AiCsScheduler.class);
                    assertThat(ctx).doesNotHaveBean(AiAutoOrderScheduler.class);
                });
    }

    @Test
    @DisplayName("ai.scheduler.enabled=true 이면 스케줄러 빈 등록")
    void beansPresentWhenPropertyTrue() {
        contextRunner
                .withPropertyValues("ai.scheduler.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(AiCsScheduler.class);
                    assertThat(ctx).hasSingleBean(AiAutoOrderScheduler.class);
                });
    }

    @Configuration
    @Import({AiCsScheduler.class, AiAutoOrderScheduler.class})
    static class AiSchedulerTestConfig {

        @Bean
        AiCsService aiCsService() {
            return mock(AiCsService.class);
        }

        @Bean
        AiAutoOrderService aiAutoOrderService() {
            return mock(AiAutoOrderService.class);
        }
    }
}
