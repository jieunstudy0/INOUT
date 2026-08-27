package com.jstudy.inout.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * prod 기동 시 DummyData* 빈이 로드되지 않도록 프로필/설정이 분리됐는지 검증한다.
 * (전체 ApplicationContext 기동 없이 어노테이션·프로퍼티만 검사 — DB/Redis 불필요)
 */
class ProdProfileSafetyTest {

    @Test
    @DisplayName("DummyDataInitializer는 local/demo/dev 에서만 활성화되고 secret을 포함하지 않는다")
    void dummyDataInitializerProfilesExcludeSecret() {
        Profile profile = DummyDataInitializer.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        List<String> values = Arrays.asList(profile.value());
        assertThat(values).containsExactlyInAnyOrder("local", "demo", "dev");
        assertThat(values).doesNotContain("secret");
    }

    @Test
    @DisplayName("DummyDataService/Scheduler도 secret 프로필을 포함하지 않는다")
    void relatedDummyBeansExcludeSecret() {
        assertThat(Arrays.asList(DummyDataService.class.getAnnotation(Profile.class).value()))
                .containsExactlyInAnyOrder("local", "demo", "dev")
                .doesNotContain("secret");
        assertThat(Arrays.asList(DummyDataScheduler.class.getAnnotation(Profile.class).value()))
                .containsExactlyInAnyOrder("local", "demo", "dev")
                .doesNotContain("secret");
    }

    @Test
    @DisplayName("기본 application.properties는 secret을 전역 include 하지 않고, group.dev로만 묶는다")
    void applicationPropertiesDoesNotGloballyIncludeSecret() throws IOException {
        // test/resources/application.properties 가 classpath에서 가리므로 메인 소스를 직접 읽는다
        String content = loadMainResourceUtf8("application.properties");
        List<String> activeLines = content.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        assertThat(activeLines)
                .as("prod에도 secret이 붙는 include는 없어야 함")
                .noneMatch(line -> line.startsWith("spring.profiles.include="));
        assertThat(activeLines)
                .as("dev 활성 시에만 secret이 함께 켜져야 함")
                .anyMatch(line -> line.equals("spring.profiles.group.dev=secret"));
    }

    @Test
    @DisplayName("application-prod.properties는 ddl validate 및 AI/예치금 스케줄러 기본 OFF")
    void prodPropertiesAreSafeDefaults() throws IOException {
        String content = loadMainResourceUtf8("application-prod.properties");
        assertThat(content).contains("spring.jpa.hibernate.ddl-auto=${DDL_AUTO:validate}");
        assertThat(content).contains("ai.scheduler.enabled=${AI_SCHEDULER_ENABLED:false}");
        assertThat(content).contains("deposit.scheduler.enabled=${DEPOSIT_SCHEDULER_ENABLED:false}");
        assertThat(content).contains("springdoc.swagger-ui.enabled=false");
        assertThat(content).contains("spring.jpa.properties.hibernate.show_sql=false");
    }

    private static String loadMainResourceUtf8(String name) throws IOException {
        Path path = Path.of("src", "main", "resources", name);
        assertThat(path).exists();
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
