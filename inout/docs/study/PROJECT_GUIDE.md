# INOUT 프로젝트 이해 가이드 (체크리스트)

> 파일별 역할 · 핵심 코드 · 반드시 이해할 점을 정리한 작업 문서.
> 처음 작성: 2026-11-20 (Claude Code와의 대화 기반) · 이후 내가 직접 고쳐 가며 쓴다.

## 사용법

- `- [ ]` 를 `- [x]` 로 바꾸며 진행한다. 항목마다 **① 코드를 직접 읽었다 → ② 내 말로 설명할 수 있다** 두 단계로 체크한다.
- `✍️ 내 설명:` 칸에는 면접에서 말하듯 **내 문장으로** 적는다. 막히는 곳이 다음에 공부할 곳이다.
- 줄 번호는 작성 시점 기준이다. 코드가 바뀌면 줄이 밀릴 수 있으니 메서드 이름으로 찾는다.
- ★★★ 파일은 코드를 직접 읽고 쓴 내용, ★★ · ★ 는 파일 이름과 [CLASS_MAP.md](CLASS_MAP.md)를 바탕으로 역할만 적은 내용이다.

## 진행 현황

- [ ] 0. 큰 그림 (한 문장 + 4층 구조)
- [ ] 1. ★★★ 파일 13묶음
- [ ] 1. ★★ 파일 흐름
- [ ] 2. 3층 정리 (30초 / 흐름 5단계 / 기술 결정 6개)
- [ ] 2. 자가 점검 질문 5개
- [ ] 2. 요청 하나 끝까지 따라가기 (디버거)

## 학습 순서

> 번호(①~⑬) 순서대로 끊어 보지 않고, **발주 한 건이 흘러가는 순서로 묶어서** 공부한다.
> ⑥(재고 락)·⑦(예치금)은 따로 떼지 않고 **흐름에서 처음 등장할 때** 같이 본다.

### 준비 단계 (CLASS_MAP "준비"와 같음)

- [ ] ⑫ 예외·응답 흐름 — 체크 6개 + 티스토리 글 2편(1번째 보강, 2번째 변경 이력으로 재작성) + `✍️ 내 설명`까지 검토받으면 완료
- [ ] `BaseTimeEntity` + `JpaAuditConfig` — 생성/수정 시각이 어떻게 자동으로 들어가는지
- [ ] `CustomUserDetails` — 컨트롤러의 `@AuthenticationPrincipal`에 로그인 사용자가 어떻게 들어오는지

### 본 학습

| 순서 | 공부할 것 | 왜 이 순서인가 |
|---|---|---|
| 1 | ① OrderStatus | 30분이면 된다. 모든 발주 코드의 `if` 문이 이 상태를 기준으로 갈리니 지도를 먼저 쥔다 |
| 2 | 요청 하나 끝까지 따라가기 | 앱을 띄우고 승인 API에 중단점을 걸어 한 번 따라간다. 세부를 보기 전에 전체 길을 걸어 본다 |
| 3 | ② 직원 기안 | 흐름의 시작점 |
| 4 | ③ 점주 결제 + ⑦ DepositService | ③이 예치금 차감을 호출하는 순간 ⑦로 들어가 매장 계좌 락까지 같이 본다 |
| 5 | ④ 본사 승인 + ⑥ Item 락 | 가장 중요한 구간. 비관적 락·`@Version`·`refresh`가 모두 여기서 쓰인다. 회귀 테스트도 같이 읽는다 |
| 6 | ⑤ 품목별 처리 | ④를 이해했다면 "돈과 재고가 짝지어 움직이는 표"만 그려 보면 된다 |
| 7 | ⑧ 배송 → ⑨ 메일 이벤트 | 승인이 끝난 뒤 일어나는 일 |
| 8 | ⑩ 보안 → ⑪ 캐시 | 흐름 밖의 공통 장치 |
| 9 | ⑬ AI | 마지막. 앞을 알아야 AI 발주 초안이 어떻게 처리되는지 보인다 |

- [ ] 1. ① OrderStatus
- [ ] 2. 요청 하나 끝까지 따라가기
- [ ] 3. ② 직원 기안
- [ ] 4. ③ 점주 결제 + ⑦ DepositService
- [ ] 5. ④ 본사 승인 + ⑥ Item 락
- [ ] 6. ⑤ 품목별 처리
- [ ] 7. ⑧ 배송 → ⑨ 메일 이벤트
- [ ] 8. ⑩ 보안 → ⑪ 캐시
- [ ] 9. ⑬ AI

### 묶음마다 같은 순서로

1. 코드 직접 읽기 → 체크 ①
2. `✍️ 내 설명` 칸에 내 문장으로 쓰기
3. 세션에 검토받기
4. 필요하면 `/code-study 클래스명`으로 티스토리 6단계 초안 만들기
5. 체크 ②

### 발주 파트 졸업 시험

본 학습 7번까지 끝나면 아래 "2. 머릿속에 정리하는 방법"의 **기술 결정 6개**와 **자가 점검 질문 5개**를 보지 않고 답해 본다.

### 실습 단계 (공부를 모두 마친 뒤)

공부를 마친 묶음부터 `/code-practice <대상>`으로 하나씩 실습한다. 실험(코드를 바꿔 보기) → 빈 파일에서 짜 보기(AI 없이 먼저) → AI가 이 영역에서 자주 틀리는 것 순서다.

- [ ] ⑫ 예외·응답 흐름
- [ ] ① ~ ⑤ 발주 흐름 (OrderStatus · 기안 · 결제 · 본사 승인 · 품목별 처리)
- [ ] ⑥ ⑦ 재고 락 · 예치금
- [ ] ⑧ ⑨ 배송 · 메일 이벤트
- [ ] ⑩ ⑪ 보안 · 캐시
- [ ] ⑬ AI

### 세션 운영

- 준비 단계는 기존 "INOUT 프로젝트 심화 학습" 세션에서 마무리한다.
- 본 학습 1번부터는 새 세션("발주 흐름 학습" 등)을 연다. 첫 메시지: "PROJECT_GUIDE.md 기준으로 ① OrderStatus부터 발주 흐름을 공부할 거야."
- 어느 세션이든 공부 형식은 메모리에, 진도는 이 문서에 남는다.

---

# 0. 큰 그림: 한 문장과 지도 한 장

**한 문장:** "본사·점주·직원이 **발주 → 예치금 결제 → 본사 승인(재고 차감) → 배송**을 처리하는 B2B 시스템이고, 핵심은 **돈과 재고가 어긋나지 않는 것**과 **다른 매장 데이터가 보이지 않는 것**입니다."

**모든 기능이 같은 4층 구조로 되어 있다.** 이것만 알면 처음 보는 파일도 어디에 속하는지 바로 알 수 있다.

```
요청 → [Controller] 주소 받고 로그인 사용자 꺼냄
          ↓
       [Service]    실제 업무 규칙 (← 공부 시간의 80%를 여기에)
          ↓
       [Repository] DB 조회 (락 거는 메서드만 중요)
          ↓
       [Entity]     테이블 = 자바 객체 (상태 바꾸는 메서드만 중요)
```

- **DTO**(`~Request`, `~Response`, `~Dto`)는 데이터를 담아 나르는 상자일 뿐이라 거의 외울 필요가 없다. 전체 파일 수의 절반 정도가 DTO다.
- 컨트롤러 이름 끝의 `Emp / Owner / Adm`은 **직원 / 점주 / 본사**용이라는 뜻이다. 같은 기능이 역할별로 세 벌씩 있어서 파일이 많아 보이는 것이다.
- 248개 파일 중 **약 20개만 깊이 알면** 나머지는 같은 모양이 반복된다.

- [ ] 한 문장을 보지 않고 말할 수 있다
- [ ] 4층 구조를 그리고, 아무 파일이나 골라 어느 층인지 말할 수 있다

✍️ 내 설명:

---

# 1. 파일별 설명

## ★★★ 반드시 이해할 파일 (면접 질문이 몰리는 곳)

### ① `OrderStatus`: 발주가 거치는 상태 순서

📄 [OrderStatus.java](../../src/main/java/com/jstudy/inout/order/entity/OrderStatus.java)

```
REQUESTED(직원 기안, 미결제) → ORDERED(점주 결제 완료) → APPROVED(본사 승인)
                    ↘ REJECTED(반려) / CANCELLED(취소)
```

**반드시 이해할 점**
- 상태마다 **돈이 빠져나갔는지** 여부가 다르다. REQUESTED에서 반려되면 돌려줄 돈이 없고, ORDERED 이후에 반려되면 **반드시 환불**해야 한다. 서비스 코드의 `if (status == ...)` 문은 거의 다 이 차이 때문에 있다.
- `isAwaitingHq()`("점주 결제는 끝났고 본사 승인을 기다리는 중")처럼 상태 판단을 enum 안에 메서드로 넣어 두었다. 그래서 서비스 코드가 "무엇을 확인하는지"를 이름만 보고 읽을 수 있다.
- `PAID`, `PARTIAL`, `COMPLETED`는 예전 버전 데이터와 호환하려고 남겨 둔 레거시 상태다.

- [ ] 코드를 직접 읽었다
- [ ] 상태 흐름을 그리고, 상태별로 "돈이 빠져나갔는지"를 말할 수 있다

✍️ 내 설명:

---

### ② `OrderEmpService`: 직원 발주 기안

📄 [OrderEmpService.java](../../src/main/java/com/jstudy/inout/order/service/OrderEmpService.java)

| 메서드 | 하는 일 |
|---|---|
| `submitOrderRequest` | 장바구니 항목으로 발주 기안 생성 (REQUESTED) |
| `cancelOrder` | 취소. 이미 결제된 건이면 환불까지 처리 |

```java
// :75-86  저장하기 "전에" 두 가지를 확인
if (!cartItem.getCart().getUser().getId().equals(userId))   // ① 내 장바구니가 맞나 → 403
    throw new InoutException(ErrorCode.NOT_OWNER, ...);
if (item.getCurrentStock() < cartItem.getQuantity())        // ② 재고가 충분한가 → 400
    throw new InoutException(ErrorCode.STOCK_SHORTAGE, ...);
```

**반드시 이해할 점**
- README 트러블슈팅 #4(남의 장바구니로 주문 가능)를 고친 코드가 바로 여기다. 확인을 저장보다 **앞에** 두었기 때문에 실패하면 주문 데이터가 한 건도 저장되지 않는다.
- `itemPriceSnapshot`은 주문한 순간의 가격을 따로 복사해 두는 값이다. 나중에 상품 가격이 바뀌어도 이미 결제한 금액과 환불할 금액이 달라지지 않는다. 면접에서 "가격 스냅샷을 왜 뒀나요?"라는 질문이 자주 나온다.

- [ ] 코드를 직접 읽었다
- [ ] 확인을 저장 앞에 둔 이유를 말할 수 있다
- [ ] 가격 스냅샷을 둔 이유를 말할 수 있다

✍️ 내 설명:

---

### ③ `OrderOwnerService`: 점주 결재와 직접 발주

📄 [OrderOwnerService.java](../../src/main/java/com/jstudy/inout/order/service/OrderOwnerService.java)

- `modifyAndApprove` ([:65](../../src/main/java/com/jstudy/inout/order/service/OrderOwnerService.java#L65)): 직원 기안의 수량을 고친 뒤 **예치금으로 결제**하고 ORDERED로 바꾼다.
- `createOwnerOrder` ([:175](../../src/main/java/com/jstudy/inout/order/service/OrderOwnerService.java#L175)): 점주가 직접 발주하고 바로 결제한다.
- `assertSameStore` ([:256](../../src/main/java/com/jstudy/inout/order/service/OrderOwnerService.java#L256)): **같은 점주 권한이라도 다른 매장 발주는 처리할 수 없게** 막는다. 다른 매장이면 403 `CROSS_STORE_FORBIDDEN`.

```java
User payer = userRepository.findByIdForUpdate(payerUserId)  // :125 사용자 행에 락
payer.consumeDailyDeposit(newTotal);                         // 직원 1일 사용 한도 차감
depositService.deductDeposit(...);                           // 매장 예치금 차감
```

**반드시 이해할 점**
- URL 규칙(`/api/owner/**`는 OWNER만)으로는 "어느 매장의 점주인지"까지는 구분하지 못한다. 그래서 **서비스에서 매장을 한 번 더 비교**한다. "데이터 격리를 어떻게 했나요?"라는 질문에 답할 핵심 코드다.

- [ ] 코드를 직접 읽었다
- [ ] URL 권한만으로 부족한 이유와 서비스에서 매장을 비교하는 이유를 말할 수 있다

✍️ 내 설명:

---

### ④ `OrderApprovalTxService`: 본사 승인 (프로젝트의 심장) 🔥

📄 [OrderApprovalTxService.java](../../src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java)

여기는 **한 줄씩 말로 설명할 수 있어야 하는 파일**이다.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)      // :50 주문 1건 = 독립 트랜잭션
public boolean processSingleOrderApproval(Long orderId, Long adminId) {
    OrderRequest order = orderRequestRepository.findByIdForUpdateWithDetails(orderId) // 주문 행에 락

    // ── 1단계: 검사만 한다 (:76-89) ──
    for (OrderDetail detail : waitingDetails) {
        Item item = lockedItems.computeIfAbsent(itemId, id -> itemRepository.findByIdWithLock(id)...);
        entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE);   // :83 최신 값 다시 읽기
        if (item.getCurrentStock() < detail.getRequestQuantity())
            throw NotEnoughStockException...;                          // 하나라도 부족하면 즉시 중단
    }
    // ── 2단계: 전부 통과했을 때만 차감한다 (:91-105) ──
    for (...) { item.removeStock(...); usageHistoryRepository.save(...); }

    deliveryService.createDeliveryIfAbsentForCompletedOrder(order);   // 배송 생성
    publishOrderStateChanged(order);                                   // 메일 이벤트 발행
    } catch (NotEnoughStockException e) {
        depositService.refundDeposit(...);  // :113 전액 환불 + 반려
```

**반드시 이해할 점 4가지**

1. **검사와 차감을 나눈 이유 (트러블슈팅 #1):** 예전에는 한 반복문에서 검사와 차감을 같이 했다. 그래서 3번째 품목에서 재고 부족이 나면 1·2번 품목은 이미 차감된 상태였고, 그 상태로 환불까지 같이 커밋됐다. 지금은 전부 확인한 뒤에만 차감한다.
   - 검증: [OrderApprovalPartialStockTest](../../src/test/java/com/jstudy/inout/order/service/OrderApprovalPartialStockTest.java) (PR #3). 예전 방식으로 되돌리면 `품목 A 재고 expected 10 but was 8`로 실패하는 것까지 확인했다(뮤테이션 검증).
2. **`refresh`를 부르는 이유 (트러블슈팅 #2):** 같은 트랜잭션에서 이미 읽어 둔 `Item`이 있으면, `SELECT ... FOR UPDATE`로 락은 잡히지만 JPA는 **메모리에 있던 예전 객체**를 돌려준다(1차 캐시). 그래서 최신 값을 강제로 다시 읽는다. "락을 잡는 것과 메모리의 값이 최신인 것은 별개"라는 문장을 꼭 말할 수 있어야 한다.
3. **`REQUIRES_NEW`를 별도 클래스로 분리한 이유:** 스프링의 `@Transactional`은 **프록시**(진짜 객체 앞에서 호출을 가로채는 대리 객체)가 처리한다. 같은 클래스 안에서 `this.메서드()`로 부르면 프록시를 거치지 않아서 새 트랜잭션이 열리지 않는다(self-invocation 문제). 그래서 `OrderAdmService`가 **다른 빈인 `OrderApprovalTxService`를 호출**하는 구조로 만들었다.
4. **일괄 승인에서 얻는 효과:** 주문 10건을 일괄 승인할 때 3번째가 실패해도 나머지 9건은 각자 따로 커밋된다. ([OrderAdmService.java:209](../../src/main/java/com/jstudy/inout/order/service/OrderAdmService.java#L209))

- [ ] 코드를 직접 읽었다
- [ ] 1. 검사/차감 분리 이유를 말할 수 있다
- [ ] 2. `refresh` 이유(1차 캐시)를 말할 수 있다
- [ ] 3. 별도 클래스 분리 이유(프록시, self-invocation)를 말할 수 있다
- [ ] 4. 일괄 승인에서 건별 독립 커밋이 되는 이유를 말할 수 있다
- [ ] 회귀 테스트를 직접 읽고, 뮤테이션 검증이 무엇인지 말할 수 있다

✍️ 내 설명:

---

### ⑤ `OrderAdmService`: 본사 승인 창구 + 품목별 처리

📄 [OrderAdmService.java](../../src/main/java/com/jstudy/inout/order/service/OrderAdmService.java)

- `approveOrder` / `rejectOrder` / `bulkApproveOrders`: ④를 호출하는 **바깥 창구**.
- `processOrderItems` ([:148](../../src/main/java/com/jstudy/inout/order/service/OrderAdmService.java#L148)): 품목마다 승인·반려를 따로 정한다. 품목을 승인하면 재고를 차감하고, 반려하면 그 품목만큼 **부분 환불**한다. 승인했던 품목을 반려로 바꾸면 재고를 되돌리고, 반려했던 품목을 승인으로 바꾸면 환불했던 돈을 다시 회수한다.

**반드시 이해할 점:** 상태가 바뀔 때마다 **돈과 재고가 반대 방향으로 짝을 이뤄 움직인다.** 이 표를 직접 그려 본다.

| 이전 → 이후 | 재고 | 예치금 |
|---|---|---|
| 대기 → 승인 | 차감 | (이미 결제됨) |
| 대기 → 반려 | 그대로 | 부분 환불 |
| 승인 → 반려 | **복구** | 부분 환불 |
| 반려 → 승인 | 차감 | **재회수** |

- [ ] 코드를 직접 읽었다
- [ ] 위 표를 보지 않고 그릴 수 있다

✍️ 내 설명:

---

### ⑥ `Item` + `ItemRepository`: 재고와 락

📄 [Item.java](../../src/main/java/com/jstudy/inout/stock/entity/Item.java) · [ItemRepository.java:40](../../src/main/java/com/jstudy/inout/stock/repository/ItemRepository.java#L40)

```java
@Version private Long version;          // 낙관적 락: "내가 읽은 뒤 누가 바꿨나?" 저장할 때 확인
@Lock(LockModeType.PESSIMISTIC_WRITE)   // 비관적 락: 읽을 때부터 다른 사람이 못 건드리게 잠금
Optional<Item> findByIdWithLock(Long id);

public void removeStock(int quantity) { // 재고가 음수가 되는 걸 엔티티가 직접 막음
    if (restStock < 0) throw NotEnoughStockException...;
}
```

**반드시 이해할 점**
- **두 락을 같이 쓴 이유:** 승인처럼 **경쟁이 심하고 틀리면 안 되는 곳**에는 비관적 락을 쓴다. 상품 정보 수정처럼 **락을 걸지 않는 경로**에서 동시에 수정이 일어나면 `@Version`이 마지막 방어선이 된다. 이때 충돌은 [GlobalExceptionHandler.java:49](../../src/main/java/com/jstudy/inout/common/config/handler/GlobalExceptionHandler.java#L49)에서 409로 바뀐다.
- `@Setter` 대신 `removeStock` 같은 **이름 있는 메서드로만** 상태를 바꾼다. 그래서 규칙(음수 금지)이 엔티티 한 곳에 모여 있다. CLAUDE.md의 규칙이기도 하다.

- [ ] 코드를 직접 읽었다
- [ ] 비관적 락과 낙관적 락의 차이, 둘을 같이 쓴 이유를 말할 수 있다
- [ ] `@Setter`를 금지한 이유를 말할 수 있다

✍️ 내 설명:

---

### ⑦ `DepositService`: 예치금 (돈)

📄 [DepositService.java](../../src/main/java/com/jstudy/inout/payment/service/DepositService.java)

```java
accountRepository.findByStoreIdForUpdate(storeId)   // :68 매장 계좌 행에 락
account.deductBalance(amount);                      // 잔액 부족이면 INSUFFICIENT_BALANCE
historyRepository.save(DepositHistory... balanceAfter(account.getBalance()))  // 거래 후 잔액 기록
```

**반드시 이해할 점**
- 계좌가 **사용자가 아니라 매장 단위**다. 같은 매장 직원 여러 명이 하나의 지갑을 쓴다. 그래서 동시 결제를 막으려면 **매장 계좌 행에 락**을 걸어야 한다.
- 잔액 숫자만 바꾸지 않고 **모든 변동을 이력(`DepositHistory`)으로 남기고**, 거래 후 잔액(`balanceAfter`)도 함께 저장한다. 은행 거래 내역과 같은 원리로, 나중에 숫자가 어긋나도 추적할 수 있다.
- `DepositLedgerConcurrencyTest`가 "100건을 동시에 결제해도 이력이 100건이고 잔액이 정확한지"를 검증한다.

- [ ] 코드를 직접 읽었다
- [ ] 매장 단위 계좌와 계좌 행 락의 관계를 말할 수 있다
- [ ] 이력을 따로 남기는 이유를 말할 수 있다

✍️ 내 설명:

---

### ⑧ `DeliveryService`: 배송 생성

📄 [DeliveryService.java:33](../../src/main/java/com/jstudy/inout/delivery/service/DeliveryService.java#L33)

- `createDeliveryIfAbsentForCompletedOrder`: 이미 배송이 있으면 **아무것도 하지 않고 끝낸다(멱등)**. 멱등은 "몇 번을 호출해도 결과가 한 번 호출한 것과 같다"는 뜻이다. 승인 경로가 여러 개(단건·품목별·일괄)라서 배송이 두 번 만들어지지 않게 막아 둔 것이다.
- 배송 상태는 READY → SHIPPING → COMPLETED 순서로 바뀌고, 단계마다 `findByOrderIdForUpdate`로 락을 건다.

- [ ] 코드를 직접 읽었다
- [ ] 멱등이 무엇이고 여기서 왜 필요한지 말할 수 있다

✍️ 내 설명:

---

### ⑨ `OrderNotificationEventListener`: 메일을 거래와 떼어 놓기

📄 [OrderNotificationEventListener.java:23-25](../../src/main/java/com/jstudy/inout/order/event/OrderNotificationEventListener.java#L23)

```java
@Async("applicationTaskExecutor")                                   // 별도 스레드에서
@Transactional(propagation = REQUIRES_NEW, readOnly = true)         // 새 트랜잭션으로 다시 조회
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 커밋 성공 후에만
public void sendOrderStateEmail(OrderStateChangedEvent event)
```

**반드시 이해할 점:** 세 줄의 어노테이션이 각각 하나의 문제를 해결한다.
- `AFTER_COMMIT`: 롤백된 거래에 대한 메일은 나가지 않는다.
- `@Async`: 메일 서버(SMTP)가 느려도 API 응답이 느려지지 않는다.
- `try/catch`: 메일이 실패해도 이미 커밋된 승인은 그대로 유지된다.
- 이벤트에 엔티티 대신 **주문 ID만** 담는 이유: 다른 스레드에서는 원래 트랜잭션이 끝났기 때문에, 새 트랜잭션에서 다시 조회해야 한다.

- [ ] 코드를 직접 읽었다
- [ ] 어노테이션 세 개가 각각 무엇을 막는지 말할 수 있다
- [ ] 이벤트에 ID만 담는 이유를 말할 수 있다

✍️ 내 설명:

---

### ⑩ 보안 4종: `SecurityConfig` → `JwtAuthenticationFilter` → `JwtTokenProvider` → `AuthLoginController`

📄 [SecurityConfig.java](../../src/main/java/com/jstudy/inout/common/config/SecurityConfig.java) · [JwtAuthenticationFilter.java](../../src/main/java/com/jstudy/inout/common/auth/filter/JwtAuthenticationFilter.java) · [JwtTokenProvider.java](../../src/main/java/com/jstudy/inout/common/jwt/JwtTokenProvider.java) · [AuthLoginController.java](../../src/main/java/com/jstudy/inout/common/auth/controller/AuthLoginController.java)

요청 하나가 지나가는 순서대로 이해한다.

```
요청 → JwtAuthenticationFilter (헤더/쿠키에서 토큰 꺼냄)
     → JwtTokenProvider.validateToken (서명·만료 확인)
     → getAuthentication (DB에서 사용자 다시 조회해 권한 세팅)
     → SecurityConfig 규칙 (/api/admin/** 은 ADMIN만 …)
     → 컨트롤러 (@AuthenticationPrincipal로 로그인 사용자 받음)
```

**반드시 이해할 점**
- [SecurityConfig.java:57](../../src/main/java/com/jstudy/inout/common/config/SecurityConfig.java#L57): `STATELESS`는 서버가 세션을 저장하지 않고 매 요청 토큰만 본다는 뜻이다. 같은 이유로 CSRF 보호도 꺼 두었다.
- [JwtTokenProvider.java:103-107](../../src/main/java/com/jstudy/inout/common/jwt/JwtTokenProvider.java#L103):
  - 리프레시 토큰에는 `roles`가 없다. 그래서 리프레시 토큰을 액세스 토큰 자리에 넣어도 인증되지 않는다.
  - 매 요청마다 **DB에서 사용자를 다시 조회**한다. 장점은 계정 잠금이나 권한 변경이 즉시 반영된다는 것, 단점은 요청마다 쿼리가 하나씩 늘어난다는 것. "JWT를 쓰는데 왜 DB를 조회하나요?"에 이 장단점으로 답한다.
- [AuthLoginController.java:140](../../src/main/java/com/jstudy/inout/common/auth/controller/AuthLoginController.java#L140): 리프레시 토큰을 DB에도 저장해 두고 대조한다. 로그아웃 시 DB에서 지우면 토큰이 바로 무효가 된다.

- [ ] 4개 파일을 직접 읽었다
- [ ] 요청 흐름을 보지 않고 그릴 수 있다
- [ ] STATELESS와 CSRF를 끈 이유를 말할 수 있다
- [ ] 리프레시 토큰이 액세스 토큰으로 쓰일 수 없는 이유를 말할 수 있다
- [ ] 매 요청 DB 조회의 장단점을 말할 수 있다

✍️ 내 설명:

---

### ⑪ 캐시 3종: `CacheConfig` · `ResilientCacheManager` · `LoggingCacheErrorHandler`

📄 [CacheConfig.java](../../src/main/java/com/jstudy/inout/common/config/CacheConfig.java) · [ResilientCacheManager.java](../../src/main/java/com/jstudy/inout/common/config/ResilientCacheManager.java) · [LoggingCacheErrorHandler.java](../../src/main/java/com/jstudy/inout/common/config/LoggingCacheErrorHandler.java)

- 대시보드·재고 목록처럼 **자주 읽고 조금 틀려도 되는** 데이터만 Redis에 3분~1시간 저장한다.

**반드시 이해할 점:** "Redis가 죽으면 어떻게 되나요?"에 대한 답. Redis 오류를 삼키고 **DB에서 바로 조회**하도록 두 겹으로 감싸 두었다([CacheConfig.java:57](../../src/main/java/com/jstudy/inout/common/config/CacheConfig.java#L57), [:66](../../src/main/java/com/jstudy/inout/common/config/CacheConfig.java#L66)). 캐시는 빨라지려고 쓰는 부가 기능이니, 캐시가 고장 나도 서비스 전체가 멈추면 안 된다는 생각이다.

- [ ] 3개 파일을 직접 읽었다
- [ ] 어떤 데이터를 캐시하고 어떤 데이터는 하지 않는지 기준을 말할 수 있다
- [ ] Redis 장애 시 동작과 "두 겹"인 이유를 말할 수 있다

✍️ 내 설명:

---

### ⑫ 예외·응답 흐름: 실패 응답이 만들어지는 두 갈래

> 클래스 4개(ErrorCode · InoutException · GlobalExceptionHandler · ResponseResult)는 뼈대일 뿐이다.
> 면접은 "401은 어디서 나가나요?", "검증 실패는 어떻게 내려가나요?"처럼 **경로**를 묻는다. 그래서 단위를 클래스가 아니라 **흐름**으로 잡는다.

**흐름 지도**

```
[길 1] 컨트롤러·서비스 안에서 실패
  서비스: throw new InoutException(ErrorCode.X)      ← ① 정의
          (하위: NotEnoughStockException, UserNotFoundException)
     ↓
  GlobalExceptionHandler (예외 종류별 핸들러 8개)      ← ② 변환
     ↓
  ResponseResult.build → ResponseMessage.fail → ResponseMessageHeader  ← ③ 포장
     (검증 실패면 body에 ResponseError 목록)
     ↓
  프론트 apiClient.parseApiError → resultCodes.js(RESULT_CODE_ACTIONS)  ← ④ 소비

[길 2] Security 필터 단계에서 막힘 (컨트롤러 도착 전)
  CustomAuthenticationEntryPoint(401) / CustomAccessDeniedHandler(403)
     → ResponseResult를 못 쓰므로 ResponseMessage.fail(ErrorCode)를 직접 사용

[지키는 장치] ErrorCodeTest (테스트 6개: 상태 범위, 문구 규칙, 상태줄 = header.status 등)
```

**깊이별 파일**

| 깊이 | 파일 | 목표 |
|---|---|---|
| 깊게 (한 줄씩) | [ErrorCode](../../src/main/java/com/jstudy/inout/common/exception/ErrorCode.java) · [InoutException](../../src/main/java/com/jstudy/inout/common/exception/InoutException.java) · [GlobalExceptionHandler](../../src/main/java/com/jstudy/inout/common/config/handler/GlobalExceptionHandler.java) · [ResponseResult](../../src/main/java/com/jstudy/inout/common/dto/ResponseResult.java) · [ResponseMessage](../../src/main/java/com/jstudy/inout/common/dto/ResponseMessage.java) | 필드·메서드 단위로 설명 |
| 중간 (역할 + 왜) | [ResponseMessageHeader](../../src/main/java/com/jstudy/inout/common/dto/ResponseMessageHeader.java) · [ResponseError](../../src/main/java/com/jstudy/inout/common/dto/ResponseError.java) · [CustomAuthenticationEntryPoint](../../src/main/java/com/jstudy/inout/common/config/handler/CustomAuthenticationEntryPoint.java) · [CustomAccessDeniedHandler](../../src/main/java/com/jstudy/inout/common/config/handler/CustomAccessDeniedHandler.java) | "왜 따로 있는지"를 말할 수 있으면 됨 |
| 얕게 (한 문장 + 위치) | [NotEnoughStockException](../../src/main/java/com/jstudy/inout/stock/exception/NotEnoughStockException.java) · [UserNotFoundException](../../src/main/java/com/jstudy/inout/common/exception/UserNotFoundException.java) · [apiClient.js](../../frontend/src/api/apiClient.js) · [resultCodes.js](../../frontend/src/utils/resultCodes.js) · [ErrorCodeTest](../../src/test/java/com/jstudy/inout/common/exception/ErrorCodeTest.java) | 질문을 받으면 어디를 보면 되는지 앎 |

**반드시 이해할 점**
- 서비스는 `throw new InoutException(ErrorCode.STOCK_SHORTAGE)`라고만 쓴다. 그러면 HTTP 상태(400), `resultCode`, 기본 문구가 **`ErrorCode` 한 곳에서** 정해진다. 호출하는 쪽은 문구만 바꿀 수 있다(예: `NotEnoughStockException.withCurrentStock`이 수량을 넣은 문구).
- `ResponseResult.build`가 `code.getStatus()` 하나를 상태줄과 `header.status` 두 자리에 넣으므로 둘이 어긋날 수 없다.
- 같은 401·403이라도 **어디서 막혔는지에 따라 처리하는 곳이 다르다.** 필터 단계(URL 규칙, 토큰 없음)는 Security 핸들러, DispatcherServlet 안(`@PreAuthorize`, 로그인 비밀번호 오류)은 `GlobalExceptionHandler`.
- 프론트는 문구가 아니라 `resultCode`로 화면 동작을 정한다. 예를 들어 `INSUFFICIENT_BALANCE`면 충전 버튼을 띄운다.
- 직접 찾아 고친 사례: [GlobalExceptionHandler.java:44](../../src/main/java/com/jstudy/inout/common/config/handler/GlobalExceptionHandler.java#L44) `@PreAuthorize` 거부가 500 → 403, [:61](../../src/main/java/com/jstudy/inout/common/config/handler/GlobalExceptionHandler.java#L61) 검증 실패 로그의 비밀번호 평문 노출 제거.
- 회고 지점: Security 핸들러는 `setStatus`와 본문 생성을 두 줄로 따로 쓴다(한쪽만 고치면 어긋날 수 있음). 또 스프링이 관리하는 `ObjectMapper`가 아니라 `new ObjectMapper()`를 써서, 응답에 날짜 필드가 생기면 스프링 설정이 적용되지 않는다.

**체크**
- [ ] "깊게" 5개 파일을 직접 읽었다
- [ ] "중간" 4개 파일을 읽고 각각 왜 따로 있는지 말할 수 있다
- [ ] "얕게" 5개 파일의 위치와 역할을 한 문장으로 말할 수 있다
- [ ] 흐름 지도를 보지 않고 두 갈래를 그릴 수 있다
- [ ] "401은 어디서 나가나요?"에 두 경우로 나눠 답할 수 있다
- [ ] 403 수정, 비밀번호 로그 수정 사례를 말할 수 있다

**티스토리 글**
- [ ] 1번째 글 「API 응답 포맷 표준화」 보강: 하위 예외(`withCurrentStock`), `parseApiError`·`RESULT_CODE_ACTIONS`, `ErrorCodeTest` 6개가 각각 막는 것. 오타 정리("단꼐", "길어진당", ErrorCode 한계 부분 존댓말 섞임)
- [ ] 2번째 글 「예외 처리 구조 통합 과정」을 **변경 이력 글**로 다시 쓰기 (옛 클래스 설명이 아니라 Before/After)
  - 문제: 응답 형식 두 개(Security는 `ErrorResponse`), resultCode가 비거나 문자열로 흩어짐, 같은 오류의 상태가 호출부마다 다름, `@PreAuthorize` 거부 500, 오버로딩으로 `resultCode: null`
  - 해결 순서(커밋): `5508a9c` 공통 응답·예외 정리 → `0175049` @PreAuthorize 403 → `b900f8a` ErrorCode 도입 → `02f9104` 검증 로그 평문 제거 → `e5489f4` ServiceResult 삭제
  - 검증: `ErrorCodeTest` · `SecurityPathAclSmokeTest` · `GlobalExceptionHandlerValidationLogTest`
  - 지금도 맞는 개념은 옮겨 쓰기: 검사/비검사 예외, `@Transactional` 기본 롤백(비검사 예외만), `@RestControllerAdvice`·`@ExceptionHandler`(자식 예외까지), `ObjectMapper`

✍️ 내 설명:

---

### ⑬ AI 4종

| 파일 | 하는 일 | 꼭 알 점 |
|---|---|---|
| [AiAutoOrderService](../../src/main/java/com/jstudy/inout/ai/service/AiAutoOrderService.java) | 자정에 재고와 최근 7일 소진량을 Gemini에 보내 발주 초안 생성 | 429/503이면 재시도([:176](../../src/main/java/com/jstudy/inout/ai/service/AiAutoOrderService.java#L176)). 최종 실패하면 **규칙 기반 계산으로 대체**([:109](../../src/main/java/com/jstudy/inout/ai/service/AiAutoOrderService.java#L109)) |
| [AiAutoOrderPersistenceService](../../src/main/java/com/jstudy/inout/ai/service/AiAutoOrderPersistenceService.java) | AI 결과를 발주 초안으로 저장 | AI가 준 itemId와 수량을 **검증한 뒤** 저장([:44-53](../../src/main/java/com/jstudy/inout/ai/service/AiAutoOrderPersistenceService.java#L44)). AI 호출(느림)과 DB 저장(트랜잭션)을 다른 클래스로 분리 |
| [AiCsService](../../src/main/java/com/jstudy/inout/ai/service/AiCsService.java) | 10분마다 문의 분류 + 답변 초안 | JSON이 깨지면 정규식으로 복구, 그래도 안 되면 기본 문구([:292](../../src/main/java/com/jstudy/inout/ai/service/AiCsService.java#L292)) |
| [AiInsightService](../../src/main/java/com/jstudy/inout/ai/service/AiInsightService.java) | 운영 요약 리포트 | `@Async` + `CompletableFuture` |

- [ ] 4개 파일을 직접 읽었다
- [ ] "AI는 초안만, 결정은 사람이" 원칙을 말할 수 있다
- [ ] AI 장애 시 대체 방식(재시도 → 규칙 계산)을 말할 수 있다
- [ ] AI 호출과 DB 저장을 다른 클래스로 나눈 이유를 말할 수 있다

✍️ 내 설명:

---

## ★★ 흐름만 알아두면 되는 파일

- [ ] **장바구니** — `CartEmpController/Service`, `Cart`, `CartDetail`: 담기·수량 변경·삭제
- [ ] **재고 관리** — `StockAdmService`: 상품 등록·수정·**논리 삭제**(`deleted=true`)·입고·실사
- [ ] **재고 사용** — `StockEmpService`: 직원 재고 사용, AI 추천 목록
- [ ] **이력** — `BaseStockHistory`, `StockReceivingHistory`, `StockUsageHistory`: 입고/사용 이력. `@MappedSuperclass`로 공통 필드를 상속
- [ ] **충전** — `ChargeService`, `ChargeRequest`: 점주 충전 신청 → 본사 승인
- [ ] **스케줄러** — `DepositLimitScheduler`, `DeliveryScheduler`, `AiAutoOrderScheduler`, `AiCsScheduler`: 자정 한도 초기화, 배송 자동 완료, AI 배치
- [ ] **소셜 로그인** — `CustomOAuth2UserService` → `OAuth2AuthenticationSuccessHandler` → `SocialOnboardingService`: 구글 로그인 → GUEST → 추가 정보 입력 후 정식 회원
- [ ] **인증 주체** — `CustomUserDetails`, `CustomUserDetailsService`: 로그인 사용자 정보를 담는 객체
- [ ] **대시보드** — `Dashboard*Service`, `DashboardAggregateService`: 역할별 집계 + 캐시
- [ ] **메일** — `MailComponent`, `MailTemplate`: DB에 저장한 템플릿으로 발송
- [ ] **설정** — `AsyncConfig`, `MdcLoggingFilter`, `JpaAuditConfig`: 비동기 스레드 풀, 요청마다 추적 ID를 로그에 붙임, 생성/수정 시각 자동 기록
- [ ] **문의·연차** — `Inquiry*`, `AnnualLeave*`: 게시판·결재. 구조는 발주와 같고 규칙만 더 단순

## ★ 이름만 알면 되는 파일

- **DTO 전부**, 락이 없는 **Repository 전부**, 단순 조회 **Controller**
- `*Runner` 6개: 서버가 뜰 때 예전 데이터를 정리하는 일회성 코드(데이터 마이그레이션)
- `DummyData*`, `MassData*`: 테스트용 데이터 생성
- `PaymentService`, `PaymentController`, `ChargeEmpController`, `OrderApprovedEvent`, `OrderDeliveryEventListener`, `AiOrderApproval*`: 레거시이거나 사용되지 않는 코드 (CLASS_MAP에 정리 대상으로 적혀 있음)

---

## 📌 코드를 읽다가 발견한 점 (회고 글 재료)

- [x] **1. README 트러블슈팅 #1을 지키는 회귀 테스트가 없었다** — `OrderApprovalPartialStockTest` 추가로 해결 (2026-11-20, PR #3). 예전 방식으로 되돌리면 실패하는 것까지 확인
- [ ] **2. `bulkApproveOrders`의 `catch (NotEnoughStockException)`은 실행될 일이 없다** — 안쪽 메서드가 이 예외를 이미 잡아서 `false`를 돌려주기 때문 · [OrderAdmService.java:219](../../src/main/java/com/jstudy/inout/order/service/OrderAdmService.java#L219) vs [OrderApprovalTxService.java:113](../../src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java#L113) · 중요도 중간
- [ ] **3. `isCancelable()`은 REQUESTED만 취소 가능하다고 하는데, 실제 `cancelOrder`는 ORDERED도 취소한다** · [OrderStatus.java:35](../../src/main/java/com/jstudy/inout/order/entity/OrderStatus.java#L35) vs [OrderEmpService.java:192](../../src/main/java/com/jstudy/inout/order/service/OrderEmpService.java#L192) · 중요도 중간
- [ ] **4. `PARTIAL`이 `@Deprecated`인데 품목별 처리에서 아직 이 값으로 바꾼다** · [OrderAdmService.java:337](../../src/main/java/com/jstudy/inout/order/service/OrderAdmService.java#L337) · 중요도 낮음
- [ ] **5. `approveOrder`는 바깥 트랜잭션 안에서 `REQUIRES_NEW`를 부르므로 요청 하나가 DB 연결을 2개 쓴다** — 동시 요청이 몰리면 커넥션 풀이 빨리 바닥남 · [OrderAdmService.java:235](../../src/main/java/com/jstudy/inout/order/service/OrderAdmService.java#L235) · 면접 심화 질문 대비
- [ ] **6. 액세스·리프레시 토큰이 `localStorage`에 저장된다** — XSS 공격을 당하면 토큰을 빼앗길 수 있음 · [LoginPage.jsx:57](../../frontend/src/pages/LoginPage.jsx#L57) · 보안 질문 대비
- [ ] **7. Gemini API 키가 URL 쿼리에 들어간다** — 서버나 프록시 로그에 키가 남을 수 있음 · [AiAutoOrderService.java:152](../../src/main/java/com/jstudy/inout/ai/service/AiAutoOrderService.java#L152) · 중요도 중간

---

# 2. 이 프로젝트를 머릿속에 정리하는 방법

내가 만든 프로젝트인데도 파악이 안 되는 건 **파일 단위로 보기 때문**이다. 면접관은 파일을 묻지 않고 **흐름과 결정**을 묻는다. 그래서 세 층으로 정리한다.

### 1층: 30초 버전 (외우기)

> "프랜차이즈 발주 시스템입니다. 직원이 기안하고, 점주가 매장 예치금으로 결제하고, 본사가 승인하면 재고가 차감되고 배송이 만들어집니다. 돈과 재고가 동시에 움직이기 때문에 정합성(숫자가 서로 어긋나지 않는 것)과 매장 간 데이터 격리에 가장 신경 썼습니다."

- [ ] 보지 않고 30초 안에 말할 수 있다

### 2층: 흐름 5단계 (그릴 수 있어야 함)

화이트보드에 **상태(REQUESTED→ORDERED→APPROVED)를 가로로 쓰고**, 각 화살표 아래에 **"누가 / 어느 서비스 / 무슨 락 / 돈과 재고가 어떻게 바뀌는지"**를 적는 연습을 한다. 이 그림 한 장이 ★★★ 파일 ②~⑨를 전부 덮는다.

- [ ] 보지 않고 그릴 수 있다

### 3층: 기술 결정 6개 (각각 "왜"를 2분 안에 말하기)

- [ ] 1. 비관적 락 + `@Version`을 같이 쓴 이유
- [ ] 2. 검사와 차감을 나눈 이유 + `refresh`를 부르는 이유
- [ ] 3. `REQUIRES_NEW`를 별도 클래스로 분리한 이유 (프록시)
- [ ] 4. 메일을 `AFTER_COMMIT` + `@Async`로 보내는 이유
- [ ] 5. Redis 장애 시 DB로 넘어가게 만든 이유
- [ ] 6. URL 권한만으로 부족해서 서비스에서 매장을 한 번 더 비교한 이유

### 공부 방법

- [ ] **요청 하나 끝까지 따라가기:** `bootRun` 후 본사 승인 API를 한 번 호출하고, `OrderApprovalTxService`에 디버거 중단점을 걸어 **컨트롤러 → 서비스 → 쿼리 로그 → 이벤트 → 메일**까지 직접 따라간다. 파일 20개를 읽는 것보다 이 한 번이 더 오래 남는다.
- 지금의 [CLASS_MAP.md](CLASS_MAP.md) 학습 순서는 좋다. 다만 **1주차 전에 "승인 1건 따라가기"를 0주차로 넣으면** 이후 공부가 수월해진다.
- 클래스 하나를 깊게 팔 때는 `/code-study OrderApprovalTxService`처럼 써서 6단계 형식으로 정리한다.

### 자가 점검 질문 (막히는 곳이 다음에 공부할 곳)

- [ ] 점주가 결제한 뒤 본사가 반려하면, 어떤 메서드가 어떤 순서로 불리나?
- [ ] 직원 두 명이 같은 매장 예치금으로 동시에 결제하면 어디서 줄을 서나?
- [ ] `processSingleOrderApproval`을 같은 클래스 안으로 옮기면 무엇이 깨지나?
- [ ] Redis를 끈 상태로 대시보드를 열면 어떤 로그가 찍히나?
- [ ] 리프레시 토큰을 액세스 토큰 자리에 넣으면 어떻게 되나?

---

# 3. 신입 채용 시장에서의 평가

### 강점: 신입 포트폴리오 중 상위권에 드는 주제

- **실무 경험과 연결됨:** 카페와 비품 관리 경험에서 나온 도메인이라 "왜 만들었나"에 설득력이 있다.
- **흔한 CRUD가 아님:** 돈·재고·동시성·트랜잭션 경계는 백엔드 면접관이 실제로 묻고 싶어 하는 주제다.
- **문제 해결 기록:** 1차 캐시 문제, self-invocation, 커밋 후 이벤트 같은 사례는 "직접 겪고 원인을 찾았다"는 증거라서 강력하다.
- **테스트 습관:** 동시성 테스트 100건과 권한 스모크 테스트는 신입 중에 드물다.

### 위험 요소

1. **가장 큰 위험은 "정말 이해하고 있나"라는 의심이다.** 규모가 크고 README에 AI 도구로 개발했다고 밝혔기 때문에, 면접관이 집요하게 파고들 것이다. 기술 결정 6개 중 하나라도 막히면 프로젝트 전체의 신뢰가 흔들린다. 그래서 지금 하는 재학습이 기능 추가보다 훨씬 가치 있다.
2. **주장과 증거가 어긋난 곳:** 동시성 테스트를 H2에서 돌린다. MySQL과 H2는 락 동작이 다르다는 걸 README에 스스로 적어 두었다. (트러블슈팅 #1 테스트 누락은 PR #3으로 해결)
3. **수치가 없음:** "빨라졌다"는 숫자(캐시 적용 전후 응답 시간, 쿼리 수)가 하나도 없다.
4. **정리되지 않은 코드:** 레거시 상태와 쓰지 않는 클래스가 남아 있다. 코드를 실제로 열어 보는 면접관에게는 감점 요인이다.

### 발전 방향 (우선순위 순)

- [ ] 1. **이해·말하기 연습** (지금 하는 것) — 위험 요소 1번 해소
- [x] 2. 트러블슈팅 #1 회귀 테스트 추가 — README의 주장에 증거가 생김 (2026-11-20, PR #3)
- [ ] 3. Testcontainers(MySQL)로 동시성 테스트 이전 — "H2라서 믿을 수 있나요?"라는 질문을 막음
- [ ] 4. 락을 itemId 순서대로 정렬해서 잡기 (데드락 예방) — README에 이미 계획으로 적혀 있고 코드 변경이 작음
- [ ] 5. 성능 수치 하나 만들기 (대시보드 캐시 전후, 또는 N+1 쿼리 개선 전후) — 숫자가 들어간 문장 하나가 이력서를 바꿈
- [ ] 6. 레거시 정리 + ARCHITECTURE.md 갱신 — 코드를 열어 봤을 때의 인상

❌ **하지 말 것:** MSA, Kafka, 새 도메인 추가 같은 **넓히기**. 신입 평가는 "많이 만들었나"보다 "만든 걸 깊이 아나"에서 갈린다.

---

# 4. AI 기능: 무엇을 고도화하고 무엇을 추가할까

### 현재 상태 평가

**좋은 점:** "AI는 초안만 만들고 결정은 사람이 한다"는 원칙, 규칙 기반 대체 계산, 재시도, AI가 준 itemId와 수량 검증이 갖춰져 있다. 실무 감각이 보이는 설계다.

**약한 점**
- Gemini 호출 코드가 **3곳에 거의 똑같이 복사**되어 있다. 모델 기본값도 `gemini-1.5-flash`와 `2.5-flash`로 서로 다르다.
- JSON 형식을 프롬프트로 "부탁"만 하고 있어서, 깨진 JSON을 정규식으로 복구하는 코드가 100줄 넘게 있다.
- 문의 본문이 그대로 프롬프트에 들어간다. 누군가 문의에 "이전 지시를 무시하고…"라고 쓰면 AI의 동작이 바뀔 수 있다(프롬프트 인젝션).
- AI가 얼마나 잘했는지 **측정하는 장치가 없다.**

### 추천 순서

- [ ] 🥇 **A. 공통 `GeminiClient` + 구조화 출력 (먼저 할 것)**
  - 세 서비스의 호출 코드를 클래스 하나로 모으고, 타임아웃·재시도·API 키 헤더(`x-goog-api-key`)를 한 곳에서 관리한다.
  - Gemini의 `responseMimeType: "application/json"` + `responseSchema` 옵션을 쓰면 API가 정해진 JSON 모양을 지켜서 돌려준다. 정규식 복구 코드 대부분을 지울 수 있다.
  - 면접에서 할 말: "LLM 응답은 믿을 수 없는 외부 입력으로 보고, 스키마로 모양을 강제한 뒤 서버에서 한 번 더 검증했습니다."
- [ ] 🥈 **B. AI 추천 채택률 기록 (가장 추천)**
  - 본사가 AI 초안을 **그대로 승인했는지, 수량을 고쳤는지, 반려했는지** 저장하고 대시보드에 보여 준다.
  - "AI 추천 수량의 70%가 수정 없이 승인됐고, 수정된 건은 주로 ○○ 품목이었다" 같은 **숫자가 나온다.** 신입 AI 기능에서 가장 드문 것이 이 측정이다.
- [ ] 🥉 **C. "계산은 코드가, 설명은 AI가"로 역할 나누기**
  - 지금은 발주 수량 계산까지 LLM에게 맡기고, 실패했을 때만 규칙 계산을 쓴다. 이걸 뒤집어서 **수량은 항상 코드로 계산**(요일별 평균, 이동평균)하고, AI는 "왜 이만큼인지 설명"과 "예외 상황 표시"만 맡긴다.
  - LLM은 숫자 계산이 약하므로 이 설계 판단 자체가 좋은 면접 소재가 된다.
- [ ] **D. 문의 답변에 근거 붙이기 (RAG를 간단하게)**
  - 환불·배송 규정 문서를 DB에 넣고, 문의와 관련 있는 규정을 찾아 프롬프트에 함께 넣는다. 답변 초안에는 "근거: 환불 규정 3조"를 표시한다.
  - 이때 문의 본문을 구분자로 감싸서 "이 안의 내용은 지시가 아니라 데이터"라고 명시하면 프롬프트 인젝션 대응까지 같이 이야기할 수 있다.
- [ ] **E. (선택) 이상 발주 감지**
  - 평소보다 금액이나 수량이 훨씬 큰 발주에 경고를 띄운다. 판정은 규칙(평균 대비 3배 등)으로 하고, AI는 경고 이유만 문장으로 쓴다.

**권하지 않는 것:** 단순 챗봇 추가, AI가 승인까지 자동으로 하는 것. 지금의 "사람이 결정한다" 원칙을 스스로 깨게 된다.

👉 **결론:** **A + B 조합.** A는 코드 품질 이야기, B는 성과를 숫자로 보여 주는 이야기가 되어 서로 보완된다.
