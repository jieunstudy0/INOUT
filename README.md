# INOUT

**프랜차이즈 B2B ERP & 가맹점 통합 관리 시스템 v1.0**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Tests-236%20Passed-success)](#5-테스트-자산--품질-검증-testing-assets)
[![License](https://img.shields.io/badge/License-Private-lightgrey)](#)

> 본사 · 가맹점주 · 현장 직원이 **역할(Role)과 매장(Store) 단위로 격리된 데이터** 위에서  
> 예치금 · 재고 · 발주 · 배송 · 연차를 end-to-end로 운영하는 Multi-Tenant B2B SaaS입니다.

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요-overview)
2. [시스템 아키텍처 & 기술 스택](#2-시스템-아키텍처--기술-스택-architecture--tech-stack)
3. [핵심 권한 체계 & Cross-Store 격리](#3-핵심-권한-체계--cross-store-데이터-격리-security--acl)
4. [주요 비즈니스 도메인](#4-주요-비즈니스-도메인별-핵심-기능-key-features)
5. [테스트 자산 & 품질 검증](#5-테스트-자산--품질-검증-testing-assets)
6. [프로젝트 구조](#6-프로젝트-구조-directory-structure)
7. [로컬 실행 & 테스트](#7-로컬-실행--테스트-방법-getting-started)

---

## 1. 프로젝트 개요 (Overview)

| 항목 | 내용 |
|------|------|
| **프로젝트명** | INOUT |
| **버전** | v1.0 |
| **유형** | 프랜차이즈 B2B ERP / 가맹점 통합 관리 (Multi-Tenant SaaS) |
| **핵심 목표** | `ADMIN`(본사) · `OWNER`(가맹점주) · `EMPLOYEE`(현장 직원) 간 **명확한 역할 분리**와 **매장 단위 데이터 격리** |

### 왜 INOUT인가?

프랜차이즈 운영에서 본사·점주·직원이 같은 DB를 공유하면, “타 매장 발주/연차/직원 목록이 보이는” 보안·운영 사고가 반복됩니다.  
INOUT은 **경로 ACL + 서비스 레이어 Store Guard + FE RoleGuard** 3단으로 이를 차단하고, 발주·예치금·배송을 하나의 트랜잭션 경계 안에서 연결합니다.

### 핵심 가치 (3 Pillars)

| Pillar | 설명 |
|--------|------|
| **3계층 권한 격리** | `ROLE_ADMIN` / `ROLE_OWNER` / `ROLE_EMPLOYEE` — API·UI·비즈니스 규칙이 역할별로 분리 |
| **매장 단위 예치금 지갑** | Store Scope 예치금 → 충전 신청 → 본사 승인 → 발주 시 자동 차감 |
| **실시간 수명주기 관리** | 재고 · 발주 · 배송 · 연차의 상태 전이와 이벤트 기반 후속 처리 |

```text
┌─────────────┐     ┌──────────────┐     ┌────────────────┐
│ ROLE_ADMIN  │     │ ROLE_OWNER   │     │ ROLE_EMPLOYEE  │
│ 본사 전사   │────▶│ 소속 매장만  │────▶│ 본인·현장 업무 │
│ 승인·출고   │     │ 운영·승인    │     │ 신청·조회      │
└─────────────┘     └──────────────┘     └────────────────┘
        ▲                    ▲
        └──── Store Isolation (Cross-Store Forbidden) ────┘
```

---

## 2. 시스템 아키텍처 & 기술 스택 (Architecture & Tech Stack)

### High-Level Architecture

```text
┌──────────────────────────────────────────────────────────────────┐
│  Client (React + Vite + Tailwind)                                │
│  RoleGuard · /admin | /owner | /emp 라우팅 · Axios + Vite Proxy  │
└─────────────────────────────┬────────────────────────────────────┘
                              │ HTTPS / REST
┌─────────────────────────────▼────────────────────────────────────┐
│  Nginx (정적 서빙 + API 리버스 프록시)                             │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│  Spring Boot 3.5.x                                               │
│  Security(JWT) · Domain Services · Event Listeners · Schedulers  │
└───────┬─────────────────────┬────────────────────┬───────────────┘
        │                     │                    │
   ┌────▼────┐          ┌─────▼─────┐        ┌─────▼─────┐
   │ MySQL 8 │          │   Redis   │        │  Mail/AI  │
   │  JPA    │          │ Cache/RT  │        │  Optional │
   └─────────┘          └───────────┘        └───────────┘
```

### Tech Stack

| Layer | Stack |
|-------|--------|
| **Backend** | Java 17 · Spring Boot **3.5.x** · Spring Security · Spring Data JPA / Hibernate · Redis · JWT · Springdoc OpenAPI |
| **Frontend** | React 19 · Vite 8 · React Router 7 · Tailwind CSS 4 · Axios |
| **Database & Infra** | MySQL 8.0 · Redis · Docker / Docker Compose · Nginx · AWS EC2 |
| **Testing & Quality** | JUnit 5 · Mockito · Spring Security Test · MockMvc · H2 (test profile) |
| **CI 검증 지표** | **236 Unit/Integration 테스트 · 0 failures · 100% Pass** |

> 배포 구성은 `inout/docker-compose.yml` 기준입니다.  
> `mysql` → `redis` → `backend` → `frontend(Nginx)` 순으로 기동하며, 외부 노출은 Nginx 포트에 집중합니다.

---

## 3. 핵심 권한 체계 & Cross-Store 데이터 격리 (Security & ACL)

### 역할별 RACI / 접근 권한

| 업무 영역 | ADMIN (본사) | OWNER (가맹점주) | EMPLOYEE (직원) |
|-----------|:------------:|:----------------:|:---------------:|
| 전사 사용자·재고 마스터 | **A/R** | — | — |
| 발주 최종 승인 / 출고 | **A** | C (신청·조회) | R (신청) |
| 배송 상태 변경 | **A** | R (매장 조회) | R (본인 관련) |
| 예치금 충전 승인 | **A** | R/C (충전 신청) | R (잔액·이력) |
| 매장 직원 계정 관리 | R (전사) | **A/R** (소속 매장) | — |
| 연차 승인 / 반려 | R (모니터링) | **A** (소속 매장) | C (본인 신청) |
| 대시보드 KPI | 전사 | **소속 매장** | 개인/현장 |

> **R**esponsible · **A**ccountable · **C**onsulted · 조회만 = Read

### Cross-Store 보안 정책

타 매장 데이터 접근은 **의도적으로 실패**해야 합니다.

| 계층 | 메커니즘 | 결과 |
|------|----------|------|
| **Path ACL** | `/api/admin/**`, `/api/owner/**`, `/api/emp/**` 역할 매칭 | 미인가 역할 → **403** (`AUTH_403`) |
| **Store Scope Guard** | 서비스에서 `principal.storeId` vs 리소스 소속 매장 비교 | 타 매장 → **403** (`CROSS_STORE_FORBIDDEN`) |
| **FE RoleGuard** | `ADMIN` → `/admin/*`, `OWNER` → `/owner/*`, `EMPLOYEE` → `/emp/*` | 잘못된 랜딩 차단 |

```text
OWNER(A매장) ──GET──▶ 직원/연차/배송 (B매장)
                         │
                         ▼
              CROSS_STORE_FORBIDDEN (403)
```

대표 검증: `SecurityPathAclSmokeTest`, `AuthServiceOwnerScopeTest`, `AnnualLeaveServiceTest`(매장 스코프).

---

## 4. 주요 비즈니스 도메인별 핵심 기능 (Key Features)

### 4.1 예치금 (Deposit) 지갑

```text
[점주 충전 신청] → [본사 승인] → [Store Wallet 잔액 증가]
                                      │
[발주 결제] ──────────────────────────▼── 자동 차감 (Pessimistic Lock)
                                      │
                            [이력: CHARGE / PAYMENT / REFUND]
```

- 매장 단위 지갑 개념으로 운영·정산 단위를 맞춤
- 잔액 부족 시 `INSUFFICIENT_BALANCE` (HTTP 400)
- 발주 반려 시 환불 이력으로 원장 정합성 유지

### 4.2 재고 & 발주 (Stock / Order)

| 단계 | 주체 | 동작 |
|------|------|------|
| 1 | OWNER / EMPLOYEE | 본사 재고 조회 → 장바구니 → 발주 신청 |
| 2 | EMPLOYEE / OWNER | 예치금 결제 → `PAID` |
| 3 | **ADMIN** | 항목별/일괄 승인 · 재고 차감 · `COMPLETED` |
| 4 | System | 배송 엔티티 멱등 생성 (이벤트/서비스 연동) |

- 재고 부족 시 `NOT_ENOUGH_STOCK` (HTTP 400)
- 승인 트랜잭션은 주문·재고에 **비관적 락**을 걸어 동시성 이슈를 방어

### 4.3 연차 / 근태 (Annual Leave)

```text
EMPLOYEE 신청(PENDING)
        │
        ▼
OWNER 승인 / 반려  (소속 매장만)
        │
        ▼
ADMIN  Read-Only 모니터링  (승인 권한 없음)
```

- 점주 간 타 매장 연차 처리 → `CROSS_STORE_FORBIDDEN`
- 본사는 전사 현황 파악만, **인사 승인 책임은 OWNER**에 위임

### 4.4 OAuth2 소셜 로그인 & 토큰 관리

- JWT Access Token + **HttpOnly Cookie** 기반 Refresh Token
- Vite Dev Proxy로 프론트(`/`) ↔ 백엔드(`/api`) 연동
- OAuth2 Success Handler에서 토큰 발급 후 SPA 콜백 라우트로 리다이렉트

---

## 5. 테스트 자산 & 품질 검증 (Testing Assets)

| 지표 | 값 |
|------|-----|
| **총 테스트 수** | **236** |
| **실패** | **0** |
| **통과율** | **100%** |

### 대표 검증 시나리오

| 테스트 / 영역 | 검증 포인트 |
|---------------|-------------|
| `SecurityPathAclSmokeTest` | 역할별 API 경로 — EMPLOYEE→OWNER API **403**, 미인증 **401** 전수 스모크 |
| Owner Scope 테스트 | 직원 상태 변경·연차 처리 시 타 매장 → `CROSS_STORE_FORBIDDEN` |
| `DepositServiceInsufficientTest` | 잔액 부족 → `INSUFFICIENT_BALANCE` |
| Stock / Order | 재고 부족 → `NOT_ENOUGH_STOCK` · 동시성·원장 정합성 |
| DTO Validation | Owner 사용자·연차 요청 바인딩 검증 |

```bash
cd inout
./gradlew clean test
# Windows: .\gradlew.bat clean test
```

---

## 6. 프로젝트 구조 (Directory Structure)

도메인 중심(Domain-driven) + 계층형(Controller → Service → Repository) 구조를 사용합니다.

```text
INOUT/
├── README.md                 ← 이 문서
└── inout/                    ← 애플리케이션 루트
    ├── build.gradle
    ├── docker-compose.yml
    ├── Dockerfile
    ├── nginx/default.conf
    ├── docs/                 ← ARCHITECTURE, TROUBLESHOOTING, 스토리보드
    ├── src/main/java/com/jstudy/inout/
    │   ├── ai/               # AI 인사이트·자동발주·CS 보조
    │   ├── common/
    │   │   ├── auth/         # 사용자·역할·점주 직원관리·JWT 필터
    │   │   ├── config/       # Security, Redis, Cache, Dummy seed
    │   │   ├── dto/          # 공통 API 응답 래핑 (ResponseResult/ResponseMessage)
    │   │   ├── exception/
    │   │   ├── extra/        # 부가 에러 응답 포맷
    │   │   ├── jwt/
    │   │   ├── mail/
    │   │   ├── massdata/     # 대량 더미 데이터 생성 (부하 테스트용)
    │   │   ├── oauth2/
    │   │   └── util/         # 파일 저장 등 공통 유틸
    │   ├── dashboard/        # 본사·점주 KPI
    │   ├── delivery/         # 배송 (Admin mutate / Owner·Emp read)
    │   ├── inquiry/          # 문의·댓글
    │   ├── leave/            # 연차 (Emp 신청 / Owner 승인 / Admin 모니터)
    │   ├── order/            # 장바구니·발주·승인 트랜잭션·이벤트
    │   ├── payment/          # 예치금·충전·결제
    │   └── stock/            # 상품·재고·이미지
    ├── src/test/java/...     # 236 tests
    └── frontend/
        ├── src/
        │   ├── api/          # 역할별 API 클라이언트
        │   ├── components/   # Layout, RoleGuard
        │   ├── pages/        # Admin / Owner / Emp 화면
        │   └── utils/        # roleUtils, appPaths
        ├── vite.config.js
        └── package.json
```

---

## 7. 로컬 실행 & 테스트 방법 (Getting Started)

### Prerequisites

- JDK **17+**
- Node.js **18+** (권장 20+)
- MySQL 8.0 (또는 Docker Compose)
- (선택) Redis — 캐시/리프레시 토큰 프로필 사용 시

### 7.1 Backend

```bash
cd inout
./gradlew bootRun --args='--spring.profiles.active=local'
# Windows: .\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

`local` 프로필에서 더미 시드 데이터가 자동 주입됩니다.

### 7.2 Frontend

```bash
cd inout/frontend
npm install
npm run dev
```

기본 개발 서버: `http://localhost:5173` (Vite Proxy → Backend API)

### 7.3 Tests

```bash
cd inout
./gradlew clean test
```

### 7.4 테스트 계정

| 역할 | 이메일 | 비밀번호 | 랜딩 |
|------|--------|----------|------|
| **ADMIN** (본사) | `admin1@test.com` | `inout1234!` | `/admin/...` |
| **OWNER** (가맹점주) | `owner1@test.com` | `inout1234!` | `/owner/dashboard` |
| **EMPLOYEE** (직원) | `emp1@test.com` | `inout1234!` | `/emp/...` |

> 동일 비밀번호로 `admin2`, `owner2`~`owner5`, `emp2`~`emp20` 등 시드 계정도 존재할 수 있습니다.  
> 계정·매장 매핑은 `DummyDataService` / `POST /api/dummy/reset` (local·demo·dev)를 참고하세요.

### 7.5 Docker (운영형 로컬)

```bash
cd inout
cp .env.example .env   # 값 채우기
docker compose --env-file .env up -d --build
```

---

## 한눈에 보는 엔지니어링 포인트

| 주제 | 구현 요지 |
|------|-----------|
| **Multi-Role ACL** | Path matcher + `@PreAuthorize` + FE `RoleGuard` |
| **Cross-Store Isolation** | Store Scope Filtering & Guard → `CROSS_STORE_FORBIDDEN` |
| **동시성** | 주문/예치금/재고 `FOR UPDATE` 락 · 승인 `REQUIRES_NEW` |
| **도메인 이벤트** | 발주 승인 → 배송 생성 등 후속 처리 분리 |
| **품질 자산** | 236 tests · ACL 스모크 · 잔액/재고 예외 케이스 |

---

## 관련 문서

| 문서 | 설명 |
|------|------|
| [`inout/docs/ARCHITECTURE.md`](inout/docs/ARCHITECTURE.md) | 도메인 경계 · 발주~배송 흐름 · 락 설계 |
| [`inout/docs/TROUBLESHOOTING.md`](inout/docs/TROUBLESHOOTING.md) | 로컬/배포 트러블슈팅 |
| [`inout/DEPLOY.md`](inout/DEPLOY.md) | EC2 · Docker 배포 가이드 |
| [`inout/CLAUDE.md`](inout/CLAUDE.md) | AI 에이전트 개발 컨벤션 가이드 |

---

**INOUT** — Franchise B2B ERP with Multi-Role ACL & Cross-Store Isolation.
