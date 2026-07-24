# ================================================================
# INOUT Spring Boot Application — Multi-Stage Docker Build
#
# 실행 예시:
#   docker build -t inout-app:latest .
#   docker run -p 8080:8080 \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/inout \
#     -e SPRING_DATASOURCE_USERNAME=inout_user \
#     -e SPRING_DATASOURCE_PASSWORD=secret \
#     -e JWT_SECRET=your-jwt-secret \
#     -e REDIS_HOST=redis \
#     -v /data/uploads:/app/uploads \
#     -v /data/logs:/app/logs \
#     inout-app:latest
# ================================================================

# ────────────────────────────────────────────────
# Stage 1: Build
# ────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Gradle Wrapper 및 빌드 스크립트 먼저 복사 (의존성 레이어 캐시 활용)
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon --quiet

# 소스 복사 및 빌드 (-x test: 테스트는 CI에서 별도 수행)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test --quiet

# ────────────────────────────────────────────────
# Stage 2: Extract layered JAR (Spring Boot Layertools)
# 의존성/스냅샷/앱 레이어를 분리 → 이미지 재빌드 속도 향상
# ────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS extractor
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ────────────────────────────────────────────────
# Stage 3: Runtime (최소 JRE 이미지)
# ────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# 보안: root 대신 전용 사용자로 실행
RUN addgroup -S inout && adduser -S inout -G inout

WORKDIR /app

# 업로드 파일 및 로그 디렉터리 생성 (볼륨 마운트 포인트)
RUN mkdir -p uploads logs && chown -R inout:inout /app

# layered JAR 레이어 순서대로 복사 (변경 빈도 낮은 것부터)
COPY --from=extractor --chown=inout:inout /app/dependencies/          ./
COPY --from=extractor --chown=inout:inout /app/spring-boot-loader/    ./
COPY --from=extractor --chown=inout:inout /app/snapshot-dependencies/ ./
COPY --from=extractor --chown=inout:inout /app/application/           ./

USER inout

EXPOSE 8080

# 컨테이너 헬스체크 (/actuator/health — SecurityConfig에서 permitAll 처리됨)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM 메모리 최적화: 컨테이너 메모리 제한 자동 감지
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
