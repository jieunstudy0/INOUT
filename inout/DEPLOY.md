# INOUT 배포 가이드 (AWS EC2 + Docker Compose)

이 문서는 INOUT 프로젝트를 AWS EC2(Amazon Linux 2023 기준) 위에 Docker Compose로 배포하기 위한
**[준비물] → [EC2 초기 세팅] → [Docker 배포] → [로그 확인]** 순서의 실행 가이드다.
이 문서만 따라 하면 별도 시행착오 없이 배포가 완료되도록 작성했다.

- 백엔드: Spring Boot 3.5.7 (Java 17) — `Dockerfile`
- 프론트엔드: React 19 + Vite → Nginx 정적 서빙 + 리버스 프록시 — `frontend/Dockerfile`, `nginx/default.conf`
- 오케스트레이션: `docker-compose.yml` (mysql, redis, backend, frontend 4개 컨테이너)
- 로컬 개발(`dev`)과 운영(`prod`)은 완전히 분리된 프로파일이며, 서로 설정이 섞이지 않는다 (§6 참고).

---

## 1. 준비물

### 1.1 AWS 리소스

| 항목 | 내용 |
|---|---|
| EC2 인스턴스 | `t3.small`(최소) 또는 `t3.medium`(권장), Amazon Linux 2023, 스토리지 20GB(gp3) 이상 |
| 보안 그룹(인바운드) | `22`(SSH, 관리자 IP만), `80`(HTTP), `443`(HTTPS, TLS 적용 시) — `3306/6379/8080`은 외부 미개방 |
| 탄력적 IP (선택) | 재부팅 시 IP 고정이 필요하면 할당 |
| 도메인 (선택) | OAuth2 소셜 로그인 콜백 등록 및 HTTPS 적용 시 필요 |

### 1.2 사전에 발급/준비해 둘 값 (`.env`에 채울 값)

이 값들이 없으면 배포 중간에 막히므로 미리 준비한다.

- MySQL 계정/비밀번호 (직접 정하면 됨 — 강력한 비밀번호로)
- JWT 서명 키 — 로컬에서 미리 생성: `openssl rand -base64 32`
- Redis 비밀번호 (직접 정하면 됨)
- 카카오 SMTP 앱 비밀번호 (카카오메일 앱 비밀번호 발급)
- Google / Kakao / Naver OAuth2 Client ID·Secret (각 개발자 콘솔에서 발급, 콜백 URI는 §6 참고)
- (선택) Gemini API 키 — 미설정 시 AI 인사이트 리포트/AI CS 자동화/AI 자동 발주 3개 기능만 비활성화되고 나머지는 정상 동작 (§9 참고)

### 1.3 로컬 PC 준비물

- SSH 클라이언트 (PowerShell 기본 `ssh` 또는 PuTTY)
- Git
- EC2 키페어(`.pem`) 파일

---

## 2. EC2 초기 세팅

### 2.1 SSH 접속

```bash
ssh -i "your-key.pem" ec2-user@<EC2 퍼블릭 IP>
```

### 2.2 Docker / Docker Compose 설치

```bash
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker   # 그룹 적용 (또는 재접속)

# Docker Compose v2 플러그인 설치
DOCKER_CONFIG=${DOCKER_CONFIG:-$HOME/.docker}
mkdir -p $DOCKER_CONFIG/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o $DOCKER_CONFIG/cli-plugins/docker-compose
chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose

docker --version
docker compose version
```

### 2.3 스왑 설정 (필수 권장)

`t3.small`(2GiB)에서 이미지를 직접 빌드하면 Gradle/npm 빌드 중 메모리 부족(OOM)이 발생할 수 있다.
2GB 스왑을 미리 만들어 둔다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h   # Swap 항목에 2.0Gi가 보이면 정상
```

### 2.4 프로젝트 코드 가져오기

```bash
git clone <repository-url> inout
cd inout/inout   # docker-compose.yml, Dockerfile이 위치한 디렉터리
```

---

## 3. Docker 배포

### 3.1 환경 변수 파일 생성

```bash
cp .env.example .env
vi .env   # §5 표를 참고해 change-me 항목을 모두 실제 값으로 교체
```

> ⚠️ `.env`는 `.gitignore`에 의해 git에 절대 커밋되지 않는다. 서버에서만 생성/보관한다.

### 3.2 최초 기동 (이미지 빌드 포함)

```bash
docker compose up -d --build
```

빌드 + 4개 컨테이너(mysql, redis, backend, frontend) 기동까지 수 분 소요될 수 있다.

### 3.3 컨테이너 상태 확인

```bash
docker compose ps
```

`STATUS` 컬럼이 모두 `Up (healthy)`로 표시되어야 한다. `backend`가 계속 재시작 중이라면 §3.4(최초 스키마 생성)를 아직 하지 않았을 가능성이 높다 — §4 로그 확인으로 원인을 먼저 특정한다.

### 3.4 최초 1회 DB 스키마 생성 (필수)

운영 기본값은 `spring.jpa.hibernate.ddl-auto=validate`이므로, 테이블이 없는 최초 배포 상태에서는
스키마 검증에 실패하며 `backend` 컨테이너가 재시작을 반복한다. **최초 1회만** 아래처럼 스키마를 생성한다.

```bash
docker compose stop backend
docker compose run --rm -e DDL_AUTO=update backend
```

- 로그에서 `Hibernate: create table ...` 및 `Started InoutApplication`(정상 기동)을 확인한 뒤 `Ctrl+C`로 종료한다.
- 확인 후 반드시 아래처럼 원복(validate)하고 정상 기동한다.

```bash
docker compose up -d backend
```

> ⚠️ `DDL_AUTO=update`를 원복하지 않으면 이후 엔티티 변경 시 운영 DB 스키마가 의도치 않게 자동 변경될 수 있다.
> 운영이 안정화된 뒤에는 Flyway/Liquibase 같은 마이그레이션 도구 도입을 권장한다.

### 3.5 배포 확인

```bash
curl -i http://<EC2 퍼블릭 IP>/actuator/health
# {"status":"UP"} 이 보이면 정상
```

브라우저로 `http://<EC2 퍼블릭 IP>/` 접속 후 로그인/상품목록/이미지 로딩까지 확인한다.

### 3.6 재배포 (코드 업데이트 시)

```bash
git pull
docker compose up -d --build       # 변경된 서비스만 재빌드 후 교체
docker image prune -f              # 이전 dangling 이미지 정리 (디스크 절약)
```

---

## 4. 로그 확인

### 4.1 기본 명령어

```bash
docker compose ps                     # 전체 컨테이너 상태 요약
docker compose logs -f backend        # 백엔드 실시간 로그 (Ctrl+C로 종료)
docker compose logs -f frontend       # Nginx 로그
docker compose logs --tail=200 mysql  # 최근 200줄만
docker stats                          # 컨테이너별 실시간 CPU/메모리 사용량
```

### 4.2 자주 발생하는 문제와 확인 방법

| 증상 | 확인 방법 | 원인/조치 |
|---|---|---|
| `backend`가 계속 재시작(`Restarting`) | `docker compose logs backend` 에서 `PlaceholderResolutionException` 또는 `Unsatisfied dependency` 검색 | `.env`에 필수 값(JWT_SECRET, SPRING_DATASOURCE_* 등) 누락 — §5 표 재확인 |
| `backend`가 `Schema-validation` 에러로 재시작 | 로그에 `Missing table` 문구 | §3.4(최초 스키마 생성)를 아직 안 함 |
| 이미지가 안 뜸(404) | 브라우저 개발자도구 Network 탭에서 `/uploads/...` 응답 코드 확인 | `nginx/default.conf`의 `/uploads/` 프록시 설정과 `WebConfig`의 `permitAll()` 매핑이 맞는지 확인 |
| 소셜 로그인 콜백 실패 | `docker compose logs backend`에서 `redirect_uri_mismatch` 검색 | OAuth2 콘솔에 등록한 콜백 URI와 `APP_FRONTEND_URL`이 일치하는지 확인 (§6) |
| `mysql`/`redis`가 `unhealthy` | `docker compose logs mysql` / `docker compose logs redis` | 비밀번호에 특수문자로 인한 파싱 오류 여부, 볼륨 권한 문제 확인 |
| 메모리 부족으로 컨테이너가 죽음(OOMKilled) | `docker compose ps` 후 `docker inspect <container> --format='{{.State.OOMKilled}}'` | §2.3 스왑 설정 여부 확인, 필요 시 `t3.medium`으로 업그레이드 |

### 4.3 컨테이너 재기동 / 초기화

```bash
docker compose restart backend        # 백엔드만 재시작
docker compose down                   # 전체 중지 (볼륨은 유지되어 데이터 보존됨)
docker compose down -v                # ⚠️ 전체 중지 + 볼륨 삭제 (DB 데이터까지 완전 초기화 — 신중히 사용)
```

---

## 5. 환경 변수 전체 목록 (`.env`)

`.env.example`을 복사해 사용하며, **change-me로 표시된 항목은 반드시 실제 값으로 교체**해야 한다.

| 변수명 | 필수 여부 | 설명 | 예시 / 기본값 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 필수 | 활성 프로파일 | `prod` |
| `MYSQL_DATABASE` | 필수 | 운영 DB 스키마명 | `inout` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 필수 | 앱 접속용 DB 계정 | 강력한 비밀번호로 교체 |
| `MYSQL_ROOT_PASSWORD` | 필수 | MySQL root 비밀번호 | 강력한 비밀번호로 교체 |
| `SPRING_DATASOURCE_URL` | 필수 (기본값 없음) | JDBC 접속 URL (compose 내부망: `jdbc:mysql://mysql:3306/inout?...`) | `.env.example` 기본값 그대로 사용 가능 |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | 필수 (기본값 없음) | 위 `MYSQL_USER`/`MYSQL_PASSWORD`와 동일 값 | - |
| `DDL_AUTO` | 선택 (평소 미설정) | 최초 스키마 생성 시에만 `update`로 1회 주입 (§3.4) | 미설정 시 `validate` |
| `JWT_SECRET` | 필수 (기본값 없음) | JWT 서명 키 (Base64 32바이트 이상 또는 hex 64자 이상) | `openssl rand -base64 32` |
| `REDIS_HOST` | 필수 (기본값 없음) | Redis 접속 호스트 — compose 내부망에서는 반드시 서비스명 `redis` | `redis` |
| `REDIS_PORT` | 선택 | Redis 포트 | 기본값 `6379` |
| `REDIS_PASSWORD` | 필수 | Redis 비밀번호 (`docker-compose.yml`의 `--requirepass`와 연동) | 강력한 비밀번호로 교체 |
| `APP_SERVER_URL` | 필수 | 백엔드 공개 URL (메일 링크 등에 사용) | `https://your-domain.com` |
| `APP_FRONTEND_URL` | 필수 | 프론트 공개 URL (OAuth2 리다이렉트 베이스) | `https://your-domain.com` |
| `CORS_ALLOWED_ORIGINS` | 필수 | 허용 오리진 (쉼표 구분 복수 가능) | `https://your-domain.com` |
| `SPRING_MAIL_PASSWORD` | 필수 (기본값 없음) | 카카오 SMTP 앱 비밀번호 | change-me |
| `GEMINI_API_KEY` | 선택 | Gemini AI API 키 — AI 인사이트 리포트 / AI CS 자동 분류·답변 초안 / AI 지능형 재고 분석·자동 발주 3개 기능이 공유 (미설정 시 3개 기능만 비활성화, 나머지 서비스는 정상 동작) | 미설정 가능 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | 필수 (기본값 없음) | Google OAuth2 앱 키 | 콘솔에서 발급 |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | 필수 (기본값 없음) | Kakao OAuth2 앱 키 | 콘솔에서 발급 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | 필수 (기본값 없음) | Naver OAuth2 앱 키 | 콘솔에서 발급 |
| `UPLOAD_PATH` | 선택 | 컨테이너 내부 업로드 경로 (볼륨 마운트 대상, 변경 불필요) | `/app/uploads` |

> "필수 (기본값 없음)"으로 표시된 변수는 `.env`에서 누락되면 컨테이너가 기동 즉시 에러를 내며 재시작을 반복한다 (§4.2 참고). 이는 의도된 동작이다 — 값이 없는 채로 조용히 잘못된 기본값(예: `localhost`)으로 기동되는 것을 막기 위함이다.

---

## 6. Nginx / OAuth2 설정 참고 사항

- 별도 환경 변수는 없으며, `frontend/Dockerfile` 빌드 시 `nginx/default.conf`가 이미지에 정적으로 포함된다.
- `backend` 서비스명이 `docker-compose.yml`과 일치해야 한다 (`upstream inout_backend { server backend:8080; }`).
- 프론트(`apiClient.js`)는 `baseURL='/api'` 상대경로를 사용하므로, 프론트 정적 파일과 백엔드 API가 **반드시 동일 오리진**으로 노출되어야 한다 — 이 Nginx 설정이 그 역할(리버스 프록시)을 담당한다.
- 도메인을 붙이고 HTTPS(TLS)를 적용하려면 `server_name`을 실제 도메인으로 바꾸고, 443 리스너 + 인증서(예: `certbot --nginx` 또는 ACM+ALB)를 추가해야 한다. 현재 설정은 HTTP(80)만 처리한다.
- OAuth2 각 플랫폼 개발자 콘솔에는 콜백 URI `https://<도메인>/login/oauth2/code/{google|kakao|naver}`를 등록해야 한다 (`APP_FRONTEND_URL`과 실제 배포 도메인이 일치해야 함).

---

## 7. 로컬 개발(dev) vs 운영(prod) 설정 분리 검증 결과

배포 준비와 별개로, 로컬 개발 환경이 운영 설정과 충돌 없이 독립적으로 동작하는지 검증하고 다음을 보완했다.

- **기본 프로파일**: `application.properties`에 `spring.profiles.active=dev`를 기본값으로 추가했다. 시스템 환경 변수/실행 옵션이 이 값보다 항상 우선하므로, Docker 배포 시 주입되는 `SPRING_PROFILES_ACTIVE=prod`가 안전하게 이 기본값을 덮어쓴다. 결과적으로 로컬은 `./gradlew bootRun`만으로 별도 설정 없이 `dev`로 기동되고, 운영은 항상 명시적으로 `prod`로 기동된다.
- **`spring.profiles.include` 위치 수정**: 기존에 `application-dev.properties` 내부에 있던 `spring.profiles.include=secret` 선언은 Spring Boot 3.x에서 `InvalidConfigDataPropertyException`을 유발하는 잘못된 위치였다 (프로필 전용 파일 안에서는 `include` 선언이 금지됨). 이를 `application.properties`(비-프로필 전용 파일)에 `spring.profiles.group.dev=secret`으로 이전해 정상 동작하도록 수정했다.
- **로컬 JWT 기본값 추가**: `application-secret.properties`(로컬 전용, git 미포함) 없이도 `./gradlew bootRun`이 바로 기동되도록 `application.properties`에 dev 전용 JWT 시크릿 기본값을 추가했다. 우선순위상 `application-secret.properties`가 존재하면 그 값이 항상 이 기본값을 덮어쓰며, `prod`는 별도로 `JWT_SECRET` 환경 변수가 없으면 여전히 기동이 차단되므로 운영 안전성에는 영향이 없다.
- **`application-prod.properties`의 로컬 전용 잔재 제거**: `spring.data.redis.host`에 남아있던 `localhost` 기본값을 제거했다. 컨테이너 환경에서 `REDIS_HOST`가 누락되면 "localhost"로 조용히 오작동하는 대신 기동 시점에 즉시 실패하도록 변경했다 (§5의 "필수(기본값 없음)" 표기와 일치).
- **검증 결과**: `SPRING_PROFILES_ACTIVE` 등 시스템 환경 변수를 전혀 설정하지 않은 상태에서 `./gradlew bootRun`을 실행해 `dev` 프로파일로 정상 기동(`Started InoutApplication ...`)됨을 직접 확인했다. (참고: 로컬에 Redis가 떠 있지 않으면 기동 후 더미데이터 초기화 단계에서 Redis 연결 오류가 발생하는데, 이는 기존과 동일한 로컬 인프라 의존성이며 이번 설정 분리 작업과는 무관하다 — 로컬 Redis를 띄우면 정상 동작한다.)

---

## 8. AWS EC2 `t3.small` 적합성 최종 점검

### 8.1 런타임(기동 후) 메모리 관점 — 사용 가능

`docker-compose.yml`에 설정된 컨테이너별 메모리 상한(`mem_limit`) 합계는 다음과 같다.

| 서비스 | mem_limit | 비고 |
|---|---|---|
| mysql | 512m | |
| redis | 160m | `maxmemory 128mb` + 오버헤드 |
| backend (Spring Boot) | 768m | `-XX:MaxRAMPercentage=75.0`로 컨테이너 한도 내 자동 조절 |
| frontend (Nginx) | 64m | |
| **합계** | **~1.5GB** | t3.small 총 2GiB 중 |

OS + Docker 데몬 오버헤드(약 300~400MB)를 감안하면 **런타임 상시 운영은 t3.small(2GiB)에서 가능**하지만
여유 메모리가 500MB 이하로 빠듯하다. 트래픽이 늘거나 배치(AI 자동발주 스케줄러 등)가 겹치는 시점에는 스와핑이 발생할 수 있다.

### 8.2 빌드(이미지 생성) 시점 — 주의 필요

`docker compose up -d --build`를 **EC2 위에서 직접 실행**하면, `Dockerfile`의 Gradle 빌드 스테이지와
`frontend/Dockerfile`의 `npm run build`가 EC2 메모리를 사용한다. Gradle 데몬 + 컴파일러가 2GiB 한도에서
간헐적으로 OOM killed될 수 있다.

**대응 방안 (택 1):**

1. **(권장) §2.3의 2GB 스왑을 미리 설정**한다. 빌드 시간이 다소 늘어나지만 EC2 사양 변경 없이 안전하게 해결된다.
2. **이미지를 로컬/CI에서 빌드해 레지스트리(ECR 등)에 push하고, EC2에서는 pull만** 한다. 가장 안전하고 배포 속도도 빠르지만 ECR 설정이 추가로 필요하다.
3. 최초 배포 시에만 `t3.medium`으로 기동해 이미지를 빌드한 뒤, 이후 `t3.small`로 다운사이징한다.

### 8.3 추가 권장 설정

- **스왑 필수**: §2.3의 2GB 스왑은 t3.small 환경에서 선택이 아닌 사실상 필수로 권장한다.
- **불필요 서비스 비활성화**: `DummyDataScheduler`/`DummyDataInitializer`는 `dev` 프로파일 전용으로 이미 제한되어 있어 `prod`에서는 실행되지 않음 — 별도 조치 불필요.
- **모니터링**: `docker stats`로 컨테이너별 실사용 메모리를 배포 직후 1~2일 관찰해 `mem_limit` 값을 재조정할 것을 권장한다.
- **로그 로테이션**: `logs-data` 볼륨이 무한정 커질 수 있으니, 운영 안정화 후 로그 보관 기간 정책을 점검한다.
- **HTTPS**: 소셜 로그인(OAuth2) 콜백 및 실제 서비스 운영을 고려하면 도메인 연결 + TLS(Let's Encrypt 등) 적용을 다음 단계로 권장한다.
- **백업**: `mysql-data` 볼륨에 대한 정기 스냅샷(EBS 스냅샷 또는 `mysqldump` cron) 설정을 권장한다.

### 8.4 결론

- **런타임**: t3.small(2GiB)에서 4개 컨테이너(mysql/redis/backend/frontend) 동시 운영이 가능하나 여유가 크지 않다. 트래픽이 적은 포트폴리오/데모 용도로는 충분하다.
- **빌드**: EC2에서 직접 이미지를 빌드할 계획이라면 스왑 설정이 사실상 필수다. 이후 운영 트래픽이 늘어난다면 `t3.medium`으로의 업그레이드를 권장한다.

---

## 9. AI 기능(Gemini) 배포 후 운영 참고

이번에 추가된 **AI 고객 문의(CS) 자동 분류·답변 초안 생성**과 **AI 지능형 재고 분석·자동 발주 제안** 기능을 배포 전 최종 점검한 결과와 운영 시 참고할 내용을 정리한다.

### 9.1 구성 요소 요약

| 기능 | 자동 실행 | 수동 트리거 API | 처리/승인 API | 대시보드 위젯 |
|---|---|---|---|---|
| AI 운영 인사이트 리포트 | 없음(수동 전용) | `GET /api/admin/ai/insight` | - | - |
| AI CS 자동 분류·답변 초안 | `AiCsScheduler` — 10분마다(KST) | `POST /api/admin/ai/cs-classify` | 문의 상세에서 `[답변란에 적용하기]` | "AI CS 처리 현황" (대기 N건 / AI 초안 완료 M건) |
| AI 지능형 재고 분석·자동 발주 | `AiAutoOrderScheduler` — 매일 자정(KST) | `POST /api/admin/ai/auto-order-analyze` | `PATCH /api/admin/orders/{orderId}/ai-approve` | "✨ AI 스마트 발주 제안" (대기 N건) |

### 9.2 권한(SecurityConfig) 검증 결과 — 수정 불필요

이번에 추가된 두 엔드포인트 모두 기존 규칙만으로 이미 안전하게 보호되고 있음을 확인했다.

- `POST /api/admin/ai/auto-order-analyze`, `PATCH /api/admin/orders/{orderId}/ai-approve` 는 둘 다 `/api/admin/**` 하위 경로이므로 `SecurityConfig`의 `.requestMatchers("/api/admin/**").hasRole("ADMIN")` 규칙에 그대로 포함된다.
- `OrderAdmController`는 클래스 레벨에 `@PreAuthorize("hasRole('ADMIN')")`가 추가로 걸려 있어, 필터 체인(SecurityConfig) + 메서드 보안(`@PreAuthorize`) 이중으로 보호된다.
- 별도 코드 수정 없이 기존 컨벤션(신규 관리자 API는 `/api/admin/**` 하위에만 두면 자동으로 보호됨)을 그대로 따르고 있어 안전하다.

### 9.3 환경 변수 — 추가로 필요한 값 없음

- 이번 AI 자동 발주 고도화로 인해 **새로 추가해야 하는 필수 환경 변수는 없다.** 기존 `GEMINI_API_KEY` 하나를 AI 인사이트 리포트/AI CS 자동화/AI 자동 발주 3개 기능이 공유한다 (§5, §1.2 참고).
- `GEMINI_API_KEY` 미설정 시: 수동 트리거 API는 예외(`InoutException`, GEMINI 미설정 코드)를 반환하고, 스케줄러는 동일 예외를 로그로만 남기고 조용히 스킵한다 — 키가 없어도 컨테이너가 재시작되거나 다른 기능이 멈추지 않는다.
- `gemini.model`(`gemini-2.5-flash`)은 `application.properties`(공통)에 고정되어 있으며 프로파일별로 override하지 않는다. 모델을 바꾸고 싶다면 환경 변수화하지 말고 이 값을 직접 수정 후 재배포하는 것을 권장한다(운영 중 실수로 바뀌는 것을 방지).

### 9.4 스케줄러 타임존 — 이번 점검에서 발견해 수정한 배포 이슈

`AiCsScheduler`, `AiAutoOrderScheduler`를 포함한 기존 4개의 `@Scheduled` cron 작업에 **타임존이 명시되어 있지 않았다.** EC2/Docker 컨테이너의 기본 OS 타임존은 대개 UTC이므로, 이 상태로 배포하면 다음과 같이 의도한 한국 시간과 9시간이 어긋나는 문제가 있었다.

| 스케줄러 | 의도한 실행 시각(KST) | 수정 전 실제 실행 시각(컨테이너가 UTC일 때) |
|---|---|---|
| `AiAutoOrderScheduler` | 매일 자정 | 매일 오전 9시 (KST) |
| `DeliveryScheduler` | 매일 오전 9시 | 매일 오후 6시 (KST) |
| `DummyDataScheduler`(dev 전용) | 매일 새벽 4시 | 매일 오후 1시 (KST) |
| `AiCsScheduler` | 10분마다 | (주기 자체는 영향 없음, 정책 일관성을 위해 동일 처리) |

**수정 내용:**

1. 4개 스케줄러의 `@Scheduled(cron = ...)`에 모두 `zone = "Asia/Seoul"`을 명시했다 — 배포 환경(컨테이너/서버 OS)의 타임존 설정과 무관하게 항상 한국 시간 기준으로 정확히 동작한다 (가장 근본적인 수정).
2. `docker-compose.yml`의 `backend` 서비스에 `TZ: Asia/Seoul` 환경 변수를 추가했다 — 스케줄러 동작 자체는 1번으로 이미 보장되지만, 로그 타임스탬프 등 컨테이너 내부의 다른 시간 표기도 한국 시간으로 통일하기 위함이다.
3. `application.properties`에 `spring.task.scheduling.pool.size=3`을 추가했다 — 기본값(1)으로는 스케줄러 1개만 동시 실행 가능해, 한 작업(예: Gemini API 응답 대기)이 길어지면 다른 스케줄러 실행이 밀릴 수 있다. 현재 최소 3개(AiCsScheduler/AiAutoOrderScheduler/DeliveryScheduler)가 상시 등록되므로 풀 크기를 늘려 서로 영향을 주지 않도록 했다.

### 9.5 배포 후 정상 동작 확인 방법

```bash
docker compose logs -f backend | grep -E "AI (CS 자동화|자동 발주)"
```

- 10분 이내에 `[AI CS 자동화 스케줄러] 실행 시작` 로그가 보이면 정상.
- 자정 무렵(로그 타임스탬프가 한국 시간 기준 00:0x인지 확인) `[AI 자동 발주 스케줄러]` 관련 로그가 보이면 정상.
- 스케줄러를 기다리지 않고 즉시 확인하려면, 관리자 로그인 후 대시보드에서 "지금 분석 실행"(AI CS) / "재고 분석 즉시 실행"(AI 자동 발주) 버튼을 눌러 수동 트리거로 바로 테스트할 수 있다.
