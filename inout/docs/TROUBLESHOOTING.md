# INOUT 트러블슈팅 & 기술 고도화 회고록

> **문서 성격**  
> 본 문서는 INOUT B2B 발주·재고·결제 시스템을 구축하면서 실제로 **100명 동시성 테스트에서 부분 차감 누수가 발생하고, 외부 메일 서버가 트랜잭션을 인질로 잡고, JPA 1차 캐시 비대칭으로 락이 무력화되며, OAuth2 설정이 프로퍼티 우선순위에 덮어써지던**  기술 문제들을 복기한 기록이다.  
> 단순 오타 수정이나 일상적인 버그 수정을 제외하고, **시스템 정합성, 성능 최적화, 아키텍처 격리** 관점에서 디버깅하며 고도화한 7가지 핵심 항목을 4단 구조로 정리했다.  
> 대상 모듈: Spring Boot 3.5.7 / Spring 6.2 / JPA(Hibernate 6) / MySQL 8 / JUnit5 + Mockito / React 19 + Vite

---

## 0. 핵심 고도화 항목 요약

| # | 분류 | 항목 | 핵심 성과 및 해결 요약 |
|---|------|------|-----------------------|
| 1 | 성능 최적화 | `OrderEmpService` Fail-Fast | 발주 INSERT 전 **비관적 락 + 재고 사전 검증**으로 무효 DB CUD 차단 |
| 2 | 성능 최적화 | `StockAdmService` 이력 중복 조회 | DB 쿼리 4회 → **2회로 축소 및 Stream 메모리 페이징/집계** |
| 3 | 트랜잭션·정합성 | `OrderApprovalTxService` 보상 트랜잭션 | **2-Pass 검증 + `EntityManager#refresh`** 로 부분 차감 누수 원천 차단 |
| 4 | 트랜잭션·정합성 | 100명 동시성 Stale Version 충돌 | JPA 1차 캐시 비대칭 해결을 위한 락 획득 후 `refresh` 강제 동기화 |
| 5 | 아키텍처 격리 | 메일 I/O 트랜잭션 강결합 해소 | `Spring Events` + `@Async` + `AFTER_COMMIT`으로 비동기 및 메인 TX 격리 |
| 6 | 아키텍처 격리 | SpringDoc `NoSuchMethodError` | Gradle `resolutionStrategy.force`로 런타임 클래스패스 버전 고정 |
| 7 | Security / OAuth2 | 소셜 로그인 환경 및 DB 제약 통합 | 프로퍼티 우선순위 교정 및 `toEntity()` 계정 필수 필드 Placeholder 적용 |

---

# SECTION 1. 성능 최적화 및 DB 접근 효율화

> 기능이 동작한다는 것과 **수십 명이 동시에 요청을 보냈을 때도 안정적으로 동작하는가**는 다른 차원의 문제다.

---

## 1.1 `OrderEmpService` — 발주 요청 시점의 Fail-Fast 재고 검증 및 N+1 차단

### 📌 문제 현상 (Issue)
기존 발주 기안(`submitOrderRequest`)은 재고 검증 없이 `OrderRequest` / `OrderDetail`을 즉시 INSERT했다. 직원 여러 명이 동일 상품을 동시 주문할 경우, 모두 `REQUESTED` 상태로 DB에 INSERT된 뒤 결제/승인 단계에서 뒤늦게 실패했다. 불필요한 DB INSERT 발생, 결제 락 경합, 사용자에게 늦은 실패 응답을 주는 문제가 존재했다. 또한 다른 사용자의 `cartDetailId`를 넘길 경우 타인의 장바구니 상품이 주문되는 소유권 검증 누락이 존재했다.

### 🔍 원인 분석 (Cause)
- **검증 시점의 지연**: DB 저장 이후 결제·승인 단계에서 비로소 재고를 확인하는 구조적 한계
- **소유권 검증 누락**: `findAllById` 사용 시 단순 ID 존재 여부만 조회하여 작성자와의 관계 미검증
- **N+1 문제**: `cartDetail.getCart().getUser().getId()` 접근 시 LAZY 프록시 로딩으로 인한 추가 쿼리 발생

### 🛠️ 해결 방법 (Solution)
1. `findWithCartAndUserByIds` fetch join을 작성해 Cart와 User 데이터를 1회 쿼리로 일괄 조회(N+1 차단)
2. 타인의 `cartDetailId` 접근 시 `403 FORBIDDEN` 예외 즉시 발생
3. `findByIdWithLock`(비관적 쓰기 락)을 적용하여 사전 재고 검증을 수행하고, 부족 시 DB INSERT 없이 즉시 `400` 응답

```java
// OrderEmpService.java - 발주 요청 전 사전 검증 로직
List<CartDetail> selectedItems = cartDetailRepository.findWithCartAndUserByIds(request.getCartDetailIds());

for (CartDetail cartItem : selectedItems) {
    if (!cartItem.getCart().getUser().getId().equals(userId)) {
        throw new InoutException("본인의 장바구니 상품만 주문할 수 있습니다.", 403, "FORBIDDEN");
    }
    Item item = itemRepository.findByIdWithLock(cartItem.getItem().getItemId())
            .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));
            
    if (item.getCurrentStock() < cartItem.getQuantity()) {
        throw new InoutException(item.getName() + " 재고가 부족합니다.", 400, "STOCK_SHORTAGE");
    }
}

```

### 💡 성과 및 깨달음 (Insight)

* 재고 부족 시 **`OrderRequest`/`OrderDetail` 테이블에 단 1건의 무효 데이터도 INSERT되지 않도록 개선**
* 발주는 본질적으로 "재고를 점유하겠다"는 요청이므로, **CUD 작업 수행 이전에 가능 여부를 먼저 결정(Fail-Fast)해야 한다**는 원칙을 정립함

---

## 1.2 `StockAdmService` — 재고 이력 중복 조회(4회 → 2회) 개선

### 📌 문제 현상 (Issue)

재고 상세 페이지(`getStockDetail`)는 이력 목록(페이징)과 누적 집계(총 입고량/사용량)를 함께 반환해야 했다. 기존 구현은 페이징용 쿼리와 집계용 쿼리를 각각 실행하여, 한 번의 API 호출에 동일한 이력 테이블을 총 **4번 SELECT**하고 있었다.

### 🔍 원인 분석 (Cause)

페이징 처리와 통계 집계 로직이 분리되어 동일한 대상 데이터를 DB에서 두 번 끌어오는 조회 중복이 원인이었다. 데이터 양이 늘어날수록 DB I/O 병목이 선형적으로 증가하는 구조였다.

### 🛠️ 해결 방법 (Solution)

* DB 조회를 입고/출고 각 1회씩 **총 2회로 단축**
* 페이징은 `Java Stream skip/limit`을 활용해 메모리 슬라이싱 처리
* 통계 집계는 이미 조회한 **동일 메모리 리스트**에서 `mapToLong(...).sum()`으로 연산

```java
// StockAdmService.java - 쿼리 단축 및 메모리 집계
List<StockHistoryResponse> allHistory = getAllHistoryForStats(itemId); // DB SELECT 2회 수행

List<StockHistoryResponse> pagedHistory = allHistory.stream()
        .skip((long) page * size)
        .limit(size)
        .collect(Collectors.toList());

return StockDetailResponse.builder()
        .history(pagedHistory)
        .totalReceived(allHistory.stream()
                .filter(h -> h.getType().equals("입고"))
                .mapToLong(StockHistoryResponse::getQuantity).sum())
        .totalUsed(allHistory.stream()
                .filter(h -> h.getType().equals("사용"))
                .mapToLong(h -> Math.abs(h.getQuantity())).sum())
        .build();

```

### 💡 성과 및 깨달음 (Insight)

* 화면당 **DB SELECT 횟수를 4회에서 2회로 50% 단축**
* 정렬 로직을 단일 메서드로 집중시켜 페이징과 집계 데이터 간 정합성 확보

---

# SECTION 2. 분산 트랜잭션 및 정합성 제어 (★ 핵심 역량)

> 동시에 100명이 같은 상품을 구매하더라도 **단 한 줄의 재고 오차나 예치금 누수가 없어야 한다.**

---

## 2.1 `OrderApprovalTxService` 보상 트랜잭션의 데이터 불일치 — 2-Pass 검증 전략

### 📌 문제 현상 (Issue)

관리자 일괄 승인 작업(`REQUIRES_NEW` 분리) 중, 한 주문 내 품목 3개 중 마지막 3번 품목의 재고가 부족한 상황이 발생했을 때:
1번 품목 차감 ➔ 2번 품목 차감 ➔ 3번 품목에서 `NotEnoughStockException` 발생 ➔ `catch` 블록에서 전액 환불 및 `REJECTED` 처리 후 커밋되는 흐름으로 작동했다.
이로 인해 **1·2번 품목은 실제 DB 재고가 차감되었음에도 손님에게는 전액 환불 처리**되는 데이터 정합성 파손이 발생했다.

### 🔍 원인 분석 (Cause)

* **선차감 + 후검증 구조**: 루프 내에서 차감과 검증을 동시에 진행하여, 중간 예외 발생 시 이전 루프에서 이미 변경된 엔티티 상태가 영속성 컨텍스트(PC)에 잔존함
* **Catch 블록 내 커밋**: 예외 발생 시 `catch` 문에서 보상 트랜잭션(환불/상태변경)을 호출하여 진행함으로써, 이전 루프의 차감 내역과 환불 처리가 한꺼번에 DB에 flush/commit됨

### 🛠️ 해결 방법 (Solution)

검증 단계와 실행 단계를 완전히 격리하는 **2-Pass 전략**을 도입했다.

* **Pass 1 (검증)**: 비관적 락 획득 + `entityManager.refresh()` + 전체 품목의 재고 수량만 검증 (DB 변경 없음)
* **Pass 2 (실행)**: 모든 품목의 재고 검증이 통과된 경우에만 실제 차감, 이력 저장, 상태 변경 진행

```java
// OrderApprovalTxService.java - Pass 1 검증과 Pass 2 실행 분리
Map<Long, Item> lockedItems = new LinkedHashMap<>();

// Pass 1: 비관적 락 및 수량 사전 검증 (데이터 변경 없음)
for (OrderDetail detail : waitingDetails) {
    Long itemId = detail.getItem().getItemId();
    Item item = lockedItems.computeIfAbsent(itemId, id ->
            itemRepository.findByIdWithLock(id).orElseThrow(...));
            
    entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE); // Stale PC 방지
    if (item.getCurrentStock() < detail.getRequestQuantity()) {
        throw NotEnoughStockException.withCurrentStock(...);
    }
}

// Pass 2: 모든 검증 통과 시에만 실제 차감 수행
for (OrderDetail detail : waitingDetails) {
    lockedItems.get(detail.getItem().getItemId()).removeStock(detail.getRequestQuantity());
    // ... 이력 저장 및 상태 변경 ...
}

```

### 💡 성과 및 깨달음 (Insight)

* N개 품목 중 단 하나라도 재고가 부족할 경우 **어떤 품목도 차감되지 않은 상태에서 안전하게 REJECTED 및 환불 처리**가 이루어짐을 검증
* **검증과 실행은 반드시 분리되어야 한다.** 실패 시점의 보상 트랜잭션이 복구해야 할 대상을 명확히 정의할 수 없다면 구조 설계의 오류임을 체감함

---

## 2.2 100명 동시성 테스트 중 JPA 영속성 컨텍스트 Stale Version 충돌

### 📌 문제 현상 (Issue)

직원 100명이 동일 상품(재고 100개)을 동시 발주하고 관리자가 100건을 동시 승인할 때, `@Version` 및 비관적 락(`findByIdWithLock`)이 적용되어 있음에도 불구하고 일부 트랜잭션에서 충돌이 발생했다.

```text
org.springframework.orm.ObjectOptimisticLockingFailureException:
    Row was updated or deleted by another transaction
    : [com.jstudy.inout.stock.entity.Item#42]

```

### 🔍 원인 분석 (Cause)

JPA 1차 캐시는 트랜잭션 단위로 유지된다. 동일 트랜잭션 내에서 특정 `Item`을 이미 조회한 이력이 있다면, 이후 `findByIdWithLock`을 호출했을 때 DB에는 `FOR UPDATE` 락을 정상적으로 잡지만, 반환되는 Java 객체 인스턴스는 1차 캐시에 남아 있던 과거 시점의 객체(Stale Version)가 반환된다.
이로 인해 flush 시점에 Hibernate가 구버전의 version으로 `UPDATE ... WHERE version=X` 쿼리를 전송하여 `OptimisticLockingFailureException`이 발생했다.

### 🛠️ 해결 방법 (Solution)

락 획득 직후 `entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE)`를 호출하여 **DB의 최신 데이터와 version 값을 영속성 컨텍스트에 강제 동기화**했다.

### 💡 성과 및 깨달음 (Insight)

* 100명 동시 승인 테스트 결과, **재고 100개에서 정확히 0으로 차감 완료 및 동시성 예외 0건 달성**
* **비관적 락 쿼리 성공이 영속성 컨텍스트(1차 캐시)의 신선도를 보장하지는 않는다.** 락을 획득한 후에는 반드시 `refresh`를 통해 캐시를 동기화해야 한다.

---

# SECTION 3. 아키텍처 격리 및 빌드 환경 구축

> 외부 시스템 장애나 빌드 환경의 버전 충돌이 메인 비즈니스 트랜잭션을 저해하지 않도록 격리해야 한다.

---

## 3.1 외부 I/O 강결합 해소 — Spring Events + `@Async` 비동기 아키텍처

### 📌 문제 현상 (Issue)

발주 상태 변경 시 이메일 발송 로직이 메인 트랜잭션 내부에서 동기로 호출되고 있었다. 이로 인해:

* SMTP 서버 응답 지연 시 API 전체 응답 시간이 수 초 이상 증가
* 메일 발송 예외 발생 시 **승인 트랜잭션 전체가 롤백**되어 재고 차감 실패
* 메일은 정상 발송되었으나 이후 DB 트랜잭션이 롤백되어 메일 내용과 실제 데이터가 불일치하는 문제 발생

### 🔍 원인 분석 (Cause)

외부 I/O 호출이라는 부수효과(Side-effect)가 메인 비즈니스 트랜잭션과 동일한 스레드/컨텍스트에서 동기 실행되는 강결합 구조가 원인이었다.

### 🛠️ 해결 방법 (Solution)

1. **이벤트 분리**: `orderId`만 포함하는 lightweight record 이벤트(`OrderStateChangedEvent`) 정의
2. **비동기 이벤트 리스너**: `@Async` 및 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 적용
3. **독립 ThreadPool 구축**: `AsyncConfig` 내 별도 ThreadPool 설정 (`corePoolSize=8, maxPoolSize=32`)

```java
// OrderNotificationEventListener.java - AFTER_COMMIT 비동기 리스너
@Async("applicationTaskExecutor")
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void sendOrderStateEmail(OrderStateChangedEvent event) {
    try {
        OrderRequest order = orderRequestRepository.findWithDetailsGraphById(event.orderId()).orElseThrow(...);
        mailComponent.sendOrderStateEmail(order);
    } catch (Exception e) {
        log.error("주문 상태 변경 메일 발송 실패: orderId={}, message={}", event.orderId(), e.getMessage(), e);
    }
}

```

### 💡 성과 및 깨달음 (Insight)

* 메일 서버 장애나 지연이 발생하더라도 메인 API 응답 속도에 영향을 주지 않음
* `AFTER_COMMIT` 설정을 통해 **DB 커밋이 완전히 성공한 건에 대해서만 메일 발송이 이루어지도록 구조적 격리 완료**
* **핵심 도메인 트랜잭션은 자기 자신의 데이터 변경에만 집중하고, 외부 부수효과는 이벤트를 통해 비동기로 분리해야 한다.**

---

## 3.2 Spring Boot 3.5 ↔ SpringDoc(Swagger) `NoSuchMethodError` 의존성 충돌

### 📌 문제 현상 (Issue)

Spring Boot 3.5.x 업그레이 후 서버 구동 시 Swagger UI 접근 과정에서 아래 예외가 발생하며 애플리케이션 시작 실패:

```text
java.lang.NoSuchMethodError:
    'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'
    at org.springdoc.core.providers.SpringDocProviders.getControllerAdvices(...)

```

### 🔍 원인 분석 (Cause)

* Spring 6.2부터 `ControllerAdviceBean(Object)` 생성자 시그니처가 삭제됨
* Gradle의 `dependency-management` 플러그인이 SpringDoc 버전을 2.5.0으로 다운그레이드하여 충돌 발생
* `build.gradle`에 2.8.6을 명시했으나 **런타임 클래스패스 상에 2.5.0 구버전 jar가 우선 배치**되어 발생한 문제

### 🛠️ 해결 방법 (Solution)

`build.gradle`에 `resolutionStrategy.force` 구문을 추가하여 런타임 의존성 버전을 강제로 고정했다.

```groovy
ext {
    springdocVersion = '2.8.6'
}

configurations.all {
    resolutionStrategy {
        force "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}",
              "org.springdoc:springdoc-openapi-starter-webmvc-api:${springdocVersion}",
              "org.springdoc:springdoc-openapi-starter-common:${springdocVersion}"
    }
}

```

### 💡 성과 및 깨달음 (Insight)

* 런타임 `NoSuchMethodError` 해결 및 Swagger UI 정상 작동 확인
* **소스 코드의 import 버전보다 실제 런타임 클래스패스에 로드되는 jar 버전이 우선함**을 확인하고, 의존성 충돌 시 `gradle dependencyInsight`를 통한 검증 절차를 정립함

---

# SECTION 4. Security & OAuth2 소셜 로그인 연동

> 환경 설정의 우선순위와 DB 엔티티 제약조건을 고려하여 안정적인 인증 체계를 구축한다.

---

## 4.1 프로퍼티 우선순위 덮어쓰기 및 소셜 신규 유저 DB 제약(NOT NULL) 예외 처리

### 📌 문제 현상 (Issue)

1. `application-secret.properties`에 Google OAuth2 실제 키가 기재되어 있음에도, 로컬(`dev` 프로필)에서 구글 로그인 시 `401 invalid_client` 에러 발생
2. 최초로 구글 로그인을 시도하는 신규 사용자의 경우 `phone` 및 `birthday` 컬럼의 `NOT NULL` 제약 조건 위반(`DataIntegrityViolationException`)으로 회원가입 실패

### 🔍 원인 분석 (Cause)

* **프로퍼티 덮어쓰기**: Spring Boot의 프로퍼티 우선순위에 의해 `application-dev.properties`가 `application.properties`보다 높은 우선순위를 가짐. `dev` 파일 내에 더미 리터럴이 하드코딩되어 있어 `secret` 파일의 실제 키가 무시됨
* **DB NOT NULL 제약**: 구글 OAuth2 응답에는 전화번호/생년월일이 포함되지 않으나, `User` 엔티티에는 해당 필드가 `nullable = false`로 지정되어 있어 발생함

### 🛠️ 해결 방법 (Solution)

1. `application-dev.properties` 내 구글 키 설정을 placeholder 방식(`${GOOGLE_CLIENT_ID:DUMMY}`)으로 변경하여 `secret` 파일 값이 존재할 경우 우선 적용되도록 수정
2. `OAuthAttributes.toEntity()` 내 소셜 로그인 신규 유저 생성 시 기본 Placeholder 값(`phone: ""`, `birthday: 1970-01-01`)을 명시적 주입

```java
// OAuthAttributes.java - 신규 소셜 유저 생성 시 Placeholder 설정
public User toEntity() {
    return User.builder()
            .email(email)
            .name(name != null ? name : "소셜사용자")
            .password(UUID.randomUUID().toString())
            .phone("")                          // 소셜 로그인 기본 Placeholder
            .birthday(LocalDate.of(1970, 1, 1)) // 소셜 로그인 기본 Placeholder
            .provider(provider)
            .providerId(providerId)
            .build();
}

```

### 💡 성과 및 깨달음 (Insight)

* 로컬 및 배포 환경에서 구글 소셜 로그인 ➔ 자동 회원가입 ➔ JWT 발급 전체 프로세스 정상화
* **프로필 전용 파일(`application-dev.properties`)이 기본 파일보다 상위 우선순위를 가짐**을 확인하고, 더미 키 배치 시에도 Placeholder 구조를 유지해야 함을 체득함
* 외부 소셜 응답 데이터와 내부 DB 제약조건 간 차이가 존재할 때는 적절한 **Placeholder 전략 또는 회원 프로필 보완 플로우**가 필수적임을 파악함

---

# 부록 A. 사고 방지 체크리스트 (개인 원칙)

1. **재고·잔액 등 자원 차감 전에는 락 획득 후 반드시 `refresh`로 영속성 컨텍스트를 최신화한다.**
2. **검증(Check)과 실행(Execute)은 2-Pass로 격리한다.** 실패 시 보상 트랜잭션이 복구할 대상을 명확히 분리해야 한다.
3. **외부 I/O(메일, 알림, 외부 API)는 비즈니스 트랜잭션 내에 두지 않는다.** `AFTER_COMMIT` 이벤트 + `@Async` 처리를 기본으로 적용한다.
4. **CUD 연산 전에는 사전 검증(Fail-Fast)을 거쳐 무효 데이터가 DB에 저장되지 않도록 방어한다.**
5. **프로필 전용 설정 파일(`application-dev.properties`)에는 하드코딩 리터럴 대신 Placeholder(`${VAR:DEFAULT}`) 구조를 사용한다.**
6. **의존성 충돌 발생 시 소스 코드 확인에 앞서 `dependencyInsight`로 실제 런타임 클래스패스를 점검한다.**

```

```