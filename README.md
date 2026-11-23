# INOUT — 프랜차이즈 발주·재고·예치금 관리 시스템

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)

본사·가맹점주·매장 직원이 하나의 시스템에서 **발주 → 결제(예치금) → 승인·재고 차감 → 배송**을 처리하는 B2B 백엔드 중심 프로젝트입니다.
핵심 관심사는 "돈과 재고가 어긋나지 않는 것"과 "다른 매장의 데이터가 보이지 않는 것"입니다.

---

## 왜 만들었나

카페에서 약 8년, 기업 비품 관리 업무를 약 1년 6개월 하면서 발주와 재고를 현장에서 직접 다뤘습니다.
이 경험을 바탕으로 세 가지를 설계 목표로 잡았습니다.

1. **동시에 여러 매장이 발주해도 재고와 예치금이 정확할 것**
2. **점주는 자기 매장 데이터만, 직원은 자기 업무만 볼 것**
3. **메일 같은 부가 기능이 실패해도 핵심 거래는 영향받지 않을 것**

---

## 핵심 흐름

```text
[직원/점주] 장바구니 → 발주 신청 ──(재고 사전 검증, 본인 장바구니 확인)
      │
      ▼
[점주] 예치금으로 결제 ──(매장 예치금 계좌 비관적 락 → 차감 → 이력 기록)
      │
      ▼
[본사] 승인 ──(품목 락 → 전 품목 검증 → 전부 통과 시에만 차감)
      │            └─ 하나라도 부족 → 전액 환불 + 반려 (재고는 그대로)
      ▼
[커밋 이후] 배송 생성 · 상태 변경 메일 (비동기, 실패해도 거래 유지)
```

| 역할 | 할 수 있는 일 |
|------|---------------|
| 본사 (ADMIN) | 상품·재고 관리, 발주 승인/반려, 배송 처리, 예치금 충전 승인, 전사 대시보드 |
| 점주 (OWNER) | 소속 매장의 발주·결제, 직원 계정 관리, 연차 승인, 매장 대시보드 |
| 직원 (EMPLOYEE) | 발주 신청, 연차 신청, 본인 관련 조회 |

---

## 문제 해결 사례

자세한 원인 분석과 코드는 [`docs/TROUBLESHOOTING.md`](inout/docs/TROUBLESHOOTING.md)에 정리했습니다.

### 1. 일부 품목만 재고가 차감된 채 전액 환불되던 문제

- **문제**: 한 주문에 품목이 3개일 때 1·2번은 차감되고 3번에서 재고 부족 예외가 나면, 예외를 잡아 환불·반려 처리하는 과정에서 앞의 차감까지 함께 커밋됐습니다. 재고는 줄었는데 돈은 전액 돌려주는 불일치였습니다.
- **원인**: 검증과 차감을 한 반복문 안에서 동시에 진행해서, 실패 시점에 이미 바뀐 엔티티가 영속성 컨텍스트에 남아 있었습니다.
- **해결**: 검증 단계와 실행 단계를 분리했습니다. 먼저 모든 품목의 락을 잡고 수량만 확인한 뒤, 전부 통과했을 때만 차감합니다.
- **코드**: [`OrderApprovalTxService`](inout/src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java)
- **검증**: [`OrderApprovalPartialStockTest`](inout/src/test/java/com/jstudy/inout/order/service/OrderApprovalPartialStockTest.java) — 마지막 품목만 재고가 부족한 주문에서 세 품목 재고가 모두 그대로이고 전액 환불되는지 확인합니다. 검증과 차감을 한 반복문으로 되돌리면 이 테스트가 실패하는 것도 확인했습니다.

### 2. 비관적 락을 잡았는데도 낙관적 락 충돌이 나던 문제

- **문제**: 100건 동시 승인 테스트에서 `ObjectOptimisticLockingFailureException`이 발생했습니다.
- **원인**: 같은 트랜잭션에서 이미 조회한 `Item`이 있으면, `SELECT ... FOR UPDATE`로 락은 잡히지만 반환되는 객체는 1차 캐시의 예전 값(이전 `@Version`)이었습니다.
- **해결**: 락 획득 직후 `entityManager.refresh(item, PESSIMISTIC_WRITE)`로 최신 상태를 다시 읽도록 했습니다.
- **배운 점**: 락을 잡는 것과 메모리의 객체가 최신인 것은 별개의 문제였습니다.

### 3. 메일 발송 실패가 발주 승인을 롤백시키던 문제

- **문제**: 상태 변경 메일을 트랜잭션 안에서 동기로 보내서, SMTP 지연이 곧 API 지연이 되고 메일 예외가 승인 롤백으로 이어졌습니다.
- **해결**: 상태 변경 이벤트(`OrderStateChangedEvent`)를 발행하고, `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 리스너에서 메일을 보냅니다. 커밋에 성공한 건만 메일이 나갑니다.
- **코드**: [`OrderNotificationEventListener`](inout/src/main/java/com/jstudy/inout/order/event/OrderNotificationEventListener.java)

### 4. 남의 장바구니 상품을 주문할 수 있던 문제

- **문제**: 발주 요청에서 장바구니 항목 ID만 확인해서, 다른 사용자의 ID를 넣으면 주문이 가능했습니다. 재고 확인도 저장 이후에 이뤄졌습니다.
- **해결**: fetch join으로 장바구니·사용자를 한 번에 조회해 소유자를 확인하고(403), 저장 전에 재고를 먼저 검증합니다(400). 재고가 부족하면 주문 데이터가 한 건도 저장되지 않습니다.

---

## 매장 단위 데이터 격리

| 단계 | 방법 | 실패 시 |
|------|------|---------|
| URL | `/api/admin/**`, `/api/owner/**`, `/api/emp/**`를 역할별로 제한 (Spring Security) | 403 |
| 서비스 | 로그인 사용자의 매장과 요청한 데이터의 매장을 비교 | 403 `CROSS_STORE_FORBIDDEN` |
| 화면 | 역할별 라우트 가드 (React) | 접근 차단 |

URL 제한만으로는 "같은 점주 권한을 가진 다른 매장" 요청을 막을 수 없어서, 서비스 계층에서 매장을 한 번 더 비교합니다.
검증 테스트: `SecurityPathAclSmokeTest`, `AuthServiceOwnerScopeTest`, `AnnualLeaveServiceTest`

---

## AI 기능 (Gemini API)

AI는 **초안만 만들고, 결정은 사람이 하도록** 설계했습니다.

| 기능 | 동작 | 실패 대비 |
|------|------|-----------|
| 자동 발주 초안 | 매일 자정 재고·판매량을 바탕으로 발주 초안 생성 → 본사가 검토 후 승인 | 429/503 시 재시도(지수 백오프), 최종 실패 시 규칙 기반 계산(안전재고 부족분 + 주간 판매량)으로 대체 |
| 문의 답변 초안 | 10분마다 대기 중인 문의에 답변 초안 작성 | AI 응답 JSON이 깨지면 정규식으로 복구, 그래도 실패하면 기본 문구 |
| 운영 인사이트 | 본사 요청 시 운영 요약 리포트를 비동기로 생성 | — |

---

## 개발 방식: AI 코딩 도구 활용과 검증

이 프로젝트는 Claude Code를 활용해 개발했습니다. 프로젝트 규칙은 [`CLAUDE.md`](inout/CLAUDE.md)에 정리해 AI가 같은 컨벤션을 따르도록 했습니다.

AI가 작성한 코드는 그대로 쓰지 않고 다음 순서로 검증했습니다.

1. 돈·재고가 바뀌는 코드는 실패 시나리오 테스트를 먼저 작성해 확인
2. 동시성이 걸린 로직은 동시 요청 테스트로 재현
3. 권한 관련 코드는 역할별 API 접근 스모크 테스트로 전수 확인

---

## 테스트

- 총 257개 (단위 테스트 Mockito, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` 통합 테스트), 실패 0건
- 동시성: 100건 동시 결제 시 예치금 이력이 누락 없이 100건 기록되고 잔액이 정확한지, 100건 동시 승인 시 재고가 정확히 0이 되고 사용 이력이 100건 남는지 검증 (`DepositLedgerConcurrencyTest`, `StockConcurrencyTest`)
- 권한: `SecurityPathAclSmokeTest`(역할별 403, 미인증 401)
- 예외: 잔액 부족 `INSUFFICIENT_BALANCE`, 재고 부족 `STOCK_SHORTAGE`

```bash
cd inout
./gradlew clean test
```

---

## 알려진 한계와 다음 개선

| 항목 | 현재 | 계획 |
|------|------|------|
| 동시성 테스트 환경 | H2에서 실행 | MySQL InnoDB와 락 동작이 달라 Testcontainers(MySQL)로 이전 |
| 락 획득 순서 | 주문 상세 순서대로 품목 락 | 서로 다른 순서로 락을 잡는 주문 간 데드락 가능성 → itemId 정렬 후 락 획득 |
| 메일 발송 실패 | 로그만 남김 | 실패 건 저장 후 재시도 |

---

## 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| Backend | Java 17, Spring Boot 3.5, Spring Security, JWT, OAuth2, Spring Data JPA, Redis |
| Database | MySQL 8 (테스트: H2) |
| Frontend | React 19, Vite, Tailwind CSS |
| Infra | Docker Compose (MySQL · Redis · Backend · Nginx), AWS EC2 |
| AI | Gemini API |
| Test | JUnit 5, Mockito, MockMvc, Spring Security Test |

배포 과정과 발생한 이슈는 [`DEPLOY_LOG.md`](inout/DEPLOY_LOG.md)에 기록했습니다.

---

## 실행 방법

```bash
# Backend (local 프로필에서 더미 데이터 자동 생성)
cd inout
./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend
cd inout/frontend
npm install && npm run dev      # http://localhost:5173

# Docker
cd inout
cp .env.example .env            # 값 채우기
docker compose --env-file .env up -d --build
```

| 역할 | 계정 | 비밀번호 |
|------|------|----------|
| 본사 | `admin1@test.com` | `inout1234!` |
| 점주 | `owner1@test.com` | `inout1234!` |
| 직원 | `emp1@test.com` | `inout1234!` |

---

## 문서

| 문서 | 내용 |
|------|------|
| [`docs/TROUBLESHOOTING.md`](inout/docs/TROUBLESHOOTING.md) | 문제 해결 상세 기록 |
| [`docs/ARCHITECTURE.md`](inout/docs/ARCHITECTURE.md) | 도메인 구조, 발주~배송 흐름, 락 설계 |
| [`DEPLOY.md`](inout/DEPLOY.md) · [`DEPLOY_LOG.md`](inout/DEPLOY_LOG.md) | EC2 배포 가이드와 이력 |
| [`CLAUDE.md`](inout/CLAUDE.md) | AI 개발 도구용 프로젝트 규칙 |
