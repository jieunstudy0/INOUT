# INOUT 트러블슈팅 & 기술 고도화 회고록

> **문서 성격**  
> 본 문서는 INOUT B2B 발주·재고·결제 시스템을 구축하면서 실제로 **컴파일이 깨지고, 100명 동시성 테스트가 부분 차감으로 무너지고, 메일 서버가 트랜잭션을 잡고 늘어졌던** 순간들을 시간 순으로 복기한 기록이다.  
> "AI가 짜준 코드를 붙여 넣은" 흔적이 아니라, 본인이 짠 코드의 결함을 **하나하나 손으로 디버깅하며 메운** 흔적이 남도록 각 항목은 `Symptom → Cause → Action → Result` 4단 구조로 정리했다.  
> 대상 모듈은 Spring Boot 3.5.7 / Spring 6.2 / JPA(Hibernate 6) / MySQL 8 / JUnit5 + Mockito 환경의 단일 모듈(`com.jstudy.inout`)이다.

---

## 0. 문서 개요 — 무엇을 왜 고쳤나

| # | 섹션 | 항목 | 한 줄 요약 |
|---|------|------|-----------|
| 1 | 코드 품질·예외 통제 | `DepositDto` 필드 불일치 | `reason` 키워드를 신설하는 대신 도메인 일관성을 위해 `description`으로 수렴 |
| 2 | 코드 품질·예외 통제 | `Item.addStock/removeStock` 음수 가드 | 도메인 모델 최후방 방어선 — 음수 인자 즉시 차단 |
| 3 | 성능 최적화 | `OrderEmpService` Fail-Fast | 발주 INSERT 이전에 **비관적 락 + 재고 사전 검증** |
| 4 | 성능 최적화 | `StockAdmService` 이력 중복 조회 | DB 2회 Hit → **1회 + Stream 메모리 페이징** |
| 5 | 트랜잭션·정합성 | `OrderApprovalTxService` 보상 트랜잭션 | **2-Pass 검증 + `EntityManager#refresh`** 로 부분 차감 누수 차단 |
| 6 | 트랜잭션·정합성 | 100명 동시성 — Stale Version 충돌 | 영속성 컨텍스트 잔존 인스턴스를 강제 재조회 |
| 7 | 아키텍처 격리 | 메일 강결합 해소 | `ApplicationEventPublisher` + `@Async` + `AFTER_COMMIT`으로 메인 TX와 격리 |
| 8 | 아키텍처 격리 | SpringDoc `NoSuchMethodError` | `resolutionStrategy.force`로 2.8.6 라인 고정 |

---

# SECTION 1. 코드 품질 및 비즈니스 예외 통제

> 잘못된 필드 한 줄, 가드 한 줄이 빠지면 **컴파일은 통과하지만 런타임에 돈이 사라진다**.  
> 이 섹션은 "기능을 추가"한 게 아니라, **있어야 했지만 빠져 있던 방어선**을 채워 넣은 기록이다.

---

## 1.1 `DepositDto` 내부 필드 불일치 에러 해결

### 1. 문제 상황 및 배경(Symptom)

발주 일괄 승인 도중 재고가 부족하면 자동 환불(보상 트랜잭션)을 호출하는 로직을 짜는 중이었다.  
처음에는 환불 사유를 `reason`이라는 이름으로 전달하고 싶어 다음과 같이 작성했다.

```java
depositService.refundDeposit(
        order.getRequestUser().getId(),
        DepositDto.RefundRequest.builder()
                .amount(order.getTotalPrice())
                .reason("재고 부족으로 인한 시스템 자동 취소 및 환불") // ← (1)
                .build()
);
```

빌드 즉시 다음과 같은 컴파일 에러가 떨어졌다.

```
error: cannot find symbol
        .reason("재고 부족으로 인한 시스템 자동 취소 및 환불")
         ^
  symbol:   method reason(java.lang.String)
  location: class DepositDto.RefundRequest.RefundRequestBuilder
```

같은 시점에 재고 실사 조정용 `StockAdmRequest`는 `reason` 필드를 쓰고 있어서, 머릿속에서 두 DTO의 네이밍이 섞이고 있던 것이 원인이었다.

### 2. 원인 분석(Cause)

`DepositDto`는 처음부터 일관되게 **충전·환불의 비고**를 `description`으로 모델링하고 있었다.  
이는 `DepositHistory` 엔티티의 컬럼명과도 일치하는 설계 결정이었다.

```5:25:inout/src/main/java/com/jstudy/inout/payment/dto/DepositDto.java
public class DepositDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChargeRequest {
        private Long amount;
        private String description;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundRequest {
        private Long amount;
        private String description;
        // 실제 운영 시에는 원본 결제(PAYMENT)의 ID를 포함하여 검증하는 것이 좋습니다.
        private Long originalHistoryId; 
    }
```

`StockAdmRequest`는 **현장 사유**(예: "오차 발견")라는 도메인적 의미를 강조해 `reason`을 쓰고 있었지만,  
예치금은 **금융성 이력의 비고**이므로 의미가 다르다.  
즉, 단순 typo가 아니라 **두 도메인의 명명 규약이 서로 다른 의도로 분리되어 있었던 것**이었고, 그것을 인지하지 못한 채 `reason`을 그대로 가져다 쓴 게 화근이었다.

여기서 두 가지 선택지가 있었다.

| 선택 | 장점 | 단점 |
|------|------|------|
| (A) `DepositDto`에 `reason` 필드 신규 추가 | 호출부 1줄만 안 바뀜 | 동일 의미의 필드가 두 개 — 어느 것이 정본인지 모호, DB 컬럼 추가 시 더 큰 부채 |
| (B) 호출부를 `description`으로 통일 | DTO 단순성 유지, 엔티티-DTO 컬럼 정렬 | 외부에서 `reason`을 보낼 가능성을 대비해 별도 alias 검토 필요 |

신입 입장에서 **"되게 만들 것인가, 옳게 만들 것인가"** 의 갈림길이었고,  
도메인 모델은 **DB 컬럼·엔티티와 같은 어휘를 쓰는 것이 장기적으로 옳다**고 판단했다. (B) 채택.

### 3. 해결 방안(Action)

호출부를 `description`으로 정정하고, 이후 같은 실수를 반복하지 않도록 메서드 docstring에 **호출 계약**을 명시했다.

**수정 전 (오류 코드)**
```java
DepositDto.RefundRequest.builder()
        .amount(order.getTotalPrice())
        .reason("재고 부족으로 인한 시스템 자동 취소 및 환불")
        .build();
```

**수정 후** — `inout/src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java`

```120:128:inout/src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java
            depositService.refundDeposit(
                    order.getRequestUser().getId(),
                    DepositDto.RefundRequest.builder()
                            .amount(order.getTotalPrice())
                            .description("재고 부족으로 인한 시스템 자동 취소 및 환불")
                            .build()
            );
```

함께 `DepositService`의 docstring을 **"호출 계약(Contract)"** 톤으로 다듬어, 다음 사람이 같은 함정을 피하도록 했다.

```17:27:inout/src/main/java/com/jstudy/inout/payment/service/DepositService.java
    /**
     * 예치금 충전 로직.
     *
     * <p><b>호출 계약:</b> {@code userId}는 반드시 현재 로그인한 사용자의 ID여야 합니다.
     * 컨트롤러에서 {@code @AuthenticationPrincipal}로 추출한 ID를 전달해야 하며,
     * 임의의 userId를 외부에서 받아 그대로 전달하면 안 됩니다.</p>
     *
     * @param userId  충전 대상 사용자 ID (반드시 본인 ID)
     * @param request 충전 요청 DTO (금액, 설명)
     */
    @Transactional
    public DepositDto.Response chargeDeposit(Long userId, DepositDto.ChargeRequest request) {
```

### 4. 최종 결과 및 검증(Result)

- 컴파일 에러 0건. `./gradlew compileJava` 정상 통과.
- 보상 트랜잭션 경로(`OrderApprovalTxService → DepositService`)에서 환불 사유가 `DepositHistory.description` 컬럼에 그대로 기록됨을 확인.
- "필드명을 통일하면 신입의 머릿속에서 두 도메인이 더 이상 헷갈리지 않는다" — 이름이 곧 설계라는 걸 체감한 사건.

---

## 1.2 `Item` 도메인의 재고 증감 메서드 음수 인자 가드 추가

### 1. 문제 상황 및 배경(Symptom)

재고 실사(`adjustStock`) 기능을 짜다 보니, 실제 재고와 전산 재고의 **차이(diff)**를 계산해서  
`diff > 0`이면 `addStock(diff)`, `diff < 0`이면 `removeStock(Math.abs(diff))`를 호출하는 구조였다.

```java
int diff = request.getActualStock() - item.getCurrentStock();
if (diff > 0) item.addStock(diff);
else if (diff < 0) item.removeStock(Math.abs(diff));
```

코드 리뷰 시점에 "만약 컨트롤러/DTO 검증을 우회해서 **음수가 도메인 메서드까지 들어가면 어떻게 되는가**?"라는 질문을 스스로 던졌다.

기존 `Item.removeStock(-5)`를 호출하면 무슨 일이 벌어지나?

```java
int restStock = this.currentStock - (-5); // = currentStock + 5
if (restStock < 0) throw …;               // 항상 false
this.currentStock = restStock;            // 결과적으로 재고가 늘어남
```

즉, **출고 메서드를 호출했는데 결과적으로 재고가 늘어나는** 의미적 버그가 잠재해 있었다. 입고도 마찬가지로 `addStock(-3)` 호출이 슬쩍 통과해 버린다.

### 2. 원인 분석(Cause)

지금까지의 가드는 모두 **상위 계층**(Bean Validation, DTO 검증, 컨트롤러 분기)에만 존재했다.

- `StockReceiveRequest`의 `quantity`에 `@Positive`가 붙어 있더라도,
- 보상 트랜잭션·내부 서비스 호출 등 **컨트롤러를 거치지 않는 경로**가 추가되면 가드가 무너진다.

DDD 관점에서 **불변식(invariant)은 엔티티의 책임**이며, "재고는 양의 정수만큼만 증감한다"는 명백한 불변식이 비어 있었다.  
도메인 모델이 자기 자신의 무결성을 책임지지 못하면, 결국 모든 호출자에 가드를 흩뿌리게 되고 그게 다음 버그의 진원지가 된다.

### 3. 해결 방안(Action)

`Item` 엔티티의 두 메서드 진입부에 **빠른 실패 가드**를 추가했다. 메시지에는 들어온 값을 그대로 포함해, 로그를 봤을 때 어디서 음수가 새어 들어왔는지 즉시 추적할 수 있게 했다.

**수정 전** — 가드 없음
```java
public void addStock(int quantity) {
    this.currentStock += quantity;
}

public void removeStock(int quantity) {
    int restStock = this.currentStock - quantity;
    if (restStock < 0) {
        throw NotEnoughStockException.withCurrentStock(this.currentStock, quantity);
    }
    this.currentStock = restStock;
}
```

**수정 후** — `inout/src/main/java/com/jstudy/inout/stock/entity/Item.java`

```94:130:inout/src/main/java/com/jstudy/inout/stock/entity/Item.java
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("입고 수량은 1 이상이어야 합니다. quantity=" + quantity);
        }
        this.currentStock += quantity;
    }

    /**
     * 재고를 감소시킵니다 (사용/출고 처리).
     * 재고 부족 시 NotEnoughStockException 을 발생시킵니다.
     * StockEmpService.useStock(), OrderAdmService.approveItemStock()에서 호출됩니다.
     *
     * <p>음수·0 인자는 도메인 모델 단에서 즉시 거부합니다.
     * "감소"라는 의미 자체가 양의 수량을 전제로 하기 때문이며,
     * 음수 인자가 들어오면 의도치 않은 재고 가산으로 둔갑할 수 있어
     * 최후방 방어선 역할을 합니다.</p>
     *
     * @param quantity 감소시킬 수량 (양수)
     * @throws IllegalArgumentException quantity가 0 이하일 때
     * @throws NotEnoughStockException 잔여 재고가 요청 수량보다 적을 때
     */
    public void removeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("출고 수량은 1 이상이어야 합니다. quantity=" + quantity);
        }
        int restStock = this.currentStock - quantity;
        if (restStock < 0) {
            throw NotEnoughStockException.withCurrentStock(this.currentStock, quantity);
        }
        this.currentStock = restStock;
    }
```

### 4. 최종 결과 및 검증(Result)

- `Item.addStock(0)`, `Item.removeStock(-3)` 등 **모든 비정상 호출**이 도메인 메서드 진입 시점에 `IllegalArgumentException`으로 차단됨을 단위 테스트(`StockAdmServiceTest`, `StockConcurrencyTest`)에서 확인.
- 상위 DTO 검증이 누락된 신규 API가 추가되더라도, 엔티티가 자기 무결성을 지키므로 **음수 차감으로 재고가 부풀려지는 사고가 구조적으로 불가능**해졌다.
- 도메인 모델이 "최후방 방어선" 역할을 하도록 만든 첫 번째 사례이자, 이후 다른 엔티티 메서드를 짤 때의 표준 패턴이 되었다.

---

# SECTION 2. 대용량 트래픽 대비 성능 최적화

> 기능이 동작한다는 것과 **수십 명이 동시에 눌렀을 때도 동작한다**는 것은 다른 차원의 문제다.  
> 이 섹션은 "성공률을 깎지 않으면서 DB 부하·메모리·트랜잭션 시간을 줄인" 두 가지 사례다.

---

## 2.1 `OrderEmpService` — 발주 요청 시점의 Fail-Fast 재고 검증

### 1. 문제 상황 및 배경(Symptom)

기존 `submitOrderRequest`는 다음 흐름이었다.

1. `cartDetailRepository.findAllById(...)`로 장바구니 상세만 조회
2. 곧바로 `OrderRequest` INSERT
3. `OrderDetail` INSERT
4. (이후 결제·관리자 승인 단계에서 비로소 재고 확인)

문제는 **"승인 단계 전까지 재고가 부족해도 발주가 일단 들어간다"** 는 것이었다.  
QA 단계에서 직원이 같은 상품을 동시에 10건 주문하면, 모두 `REQUESTED` 상태로 INSERT가 끝난 뒤 결제 단계에서 우르르 실패하는 시나리오가 재현됐다. **불필요한 DB INSERT 10건 + 결제 락 경합 + 사용자에게는 늦은 실패 응답**이라는 최악의 조합이었다.

추가로, `cartDetail`에 적재된 사용자가 **본인이 맞는지 검증조차 없이** 그대로 주문이 만들어지는 구멍이 있었다.

### 2. 원인 분석(Cause)

- **재고 검증 시점이 너무 늦었다.** 발주는 본질적으로 "재고를 점유하겠다"는 의사 표시이므로, **INSERT 이전에 가능 여부를 결정**해야 한다.
- **소유권 검증 누락.** `findAllById`는 cartDetailId만 보고 가져오므로, 다른 사용자의 cartDetailId를 알아내면 그 사람의 장바구니로도 주문이 가능했다.
- **N+1 문제.** `cd.getCart().getUser().getId()` 호출 시점에 LAZY 프록시가 풀리면서 N개의 추가 쿼리가 발생할 위험이 있었다.

### 3. 해결 방안(Action)

세 가지 변경을 한 번에 적용했다.

1. `findWithCartAndUserByIds`로 **fetch join** 조회 → Cart·User까지 한 번에 가져와 N+1 차단
2. 본인 장바구니 검증 — 다른 사용자의 cartDetail이면 즉시 403
3. **상품별 `findByIdWithLock`(비관적 쓰기 락) + 현재 재고 충분 여부 확인** → 부족하면 즉시 400으로 실패 (DB INSERT 없음)

**수정 전 (`OrderEmpService.submitOrderRequest`)**
```java
List<CartDetail> selectedItems = cartDetailRepository.findAllById(request.getCartDetailIds());
if (selectedItems.isEmpty()) throw new InoutException("발주할 상품이 없습니다.", 400, "EMPTY_ORDER");

long calculatedTotalPrice = selectedItems.stream()
        .mapToLong(cd -> cd.getItem().getUnitPrice() * cd.getQuantity())
        .sum();

OrderRequest orderRequest = OrderRequest.builder()
        .requestUser(user)
        .status(OrderStatus.REQUESTED)
        .totalPrice(calculatedTotalPrice)
        .requestDate(LocalDateTime.now())
        .build();

orderRequestRepository.save(orderRequest);
```

**수정 후** — `inout/src/main/java/com/jstudy/inout/order/service/OrderEmpService.java`

```56:80:inout/src/main/java/com/jstudy/inout/order/service/OrderEmpService.java
    @Transactional
    public void submitOrderRequest(Long userId, OrderCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"));

        List<CartDetail> selectedItems = cartDetailRepository.findWithCartAndUserByIds(request.getCartDetailIds());

        if (selectedItems.isEmpty()) {
            throw new InoutException("발주할 상품이 없습니다.", 400, "EMPTY_ORDER");
        }

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

> 비관적 락(`SELECT ... FOR UPDATE`)을 굳이 발주 요청 시점에 잡는 이유는 두 가지다.  
> ① 재고 검증과 차감(승인 단계) 사이의 시간이 길어도, **검증 시점에는 적어도 그 트랜잭션 동안의 일관성**을 보장하기 위해.  
> ② 같은 상품을 동시에 발주하려는 N개의 직원 요청 중, **재고가 정말 부족한 케이스를 가장 먼저 실패 응답**으로 떨궈주기 위해.

### 4. 최종 결과 및 검증(Result)

- 발주 요청 단계에서 재고 부족이면 `OrderRequest`/`OrderDetail`이 **단 한 줄도 INSERT되지 않음**을 확인.
- 다른 사용자의 cartDetailId 위·변조 시도는 `403 FORBIDDEN`으로 차단.
- `StockConcurrencyTest` — 직원 100명이 재고 100개짜리 상품에 동시 발주 시, 모두 정상 REQUESTED 상태로 진입하고 이후 승인 단계에서 정확히 100개 차감되어 0이 됨을 검증.

---

## 2.2 `StockAdmService` — 재고 이력 중복 조회(N+1성 부하) 개선

### 1. 문제 상황 및 배경(Symptom)

`getStockDetail(itemId, page, size)`는 화면 한 번에 다음 두 가지를 같이 그려야 했다.

- **이력 테이블** (페이징된 입고+사용 이력)
- **누적 집계** (총 입고량, 총 사용량)

기존 구현은 이 두 가지를 위해 **사실상 같은 데이터를 DB에서 두 번 끌어오고** 있었다.

```java
// 화면 표시용: 페이징 적용된 이력
List<StockHistoryResponse> pagedHistory = getUnifiedHistory(itemId, page, size);
// 집계용: 전체 이력 (totalReceived, totalUsed 계산에 사용)
List<StockHistoryResponse> allHistory = getAllHistoryForStats(itemId);
```

내부적으로 `getUnifiedHistory`도 `getAllHistoryForStats`와 마찬가지로 **`receiving 전체 + usage 전체` 두 쿼리**를 날리고 있었다. 즉, 화면 한 번에 **`receiving × 2 + usage × 2` = 총 4번의 SELECT**가 나가는 셈이었다.

### 2. 원인 분석(Cause)

페이징 책임과 집계 책임을 같은 메서드에서 처리하려고 욕심을 부린 결과,  
"페이징을 위해 한 번, 집계를 위해 또 한 번" 동일 데이터를 **DB에서 거듭 끌어오는 N+1성 비효율**이 발생했다.

DB에 100만 건이 누적된 상황을 시뮬레이션하니, 한 번의 상세 조회로 동일한 행을 두 번 직렬화·전송·매핑하면서 **응답 시간이 거의 2배**로 늘어났다. 데이터 양이 더 커지면 페이징의 이점이 무색해진다.

### 3. 해결 방안(Action)

핵심 아이디어는 단순했다.

- DB 쿼리는 **딱 한 번** 친다. (`receivingHistoryRepository.findAllByItem_ItemId` + `usageHistoryRepository.findAllByItem_ItemId`)
- 정렬도 그때 같이 끝낸다. (`Comparator.comparing(...).reversed()`)
- 페이징은 **Java Stream의 `skip/limit`** 로 메모리에서 슬라이싱한다.
- 집계는 **같은 리스트**에서 `mapToLong(...).sum()`으로 함께 계산한다.

**수정 전 (`StockAdmService.getStockDetail`)** — 페이징·집계 따로
```java
List<StockHistoryResponse> pagedHistory = getUnifiedHistory(itemId, page, size); // 내부에서 DB 2회
List<StockHistoryResponse> allHistory   = getAllHistoryForStats(itemId);          // 다시 DB 2회
```

**수정 후** — `inout/src/main/java/com/jstudy/inout/stock/service/StockAdmService.java`

```249:283:inout/src/main/java/com/jstudy/inout/stock/service/StockAdmService.java
    @Transactional(readOnly = true)
    public StockDetailResponse getStockDetail(Long itemId, int page, int size) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new InoutException("상품을 찾을 수 없습니다.", 404, "ITEM_NOT_FOUND"));

        List<StockHistoryResponse> allHistory = getAllHistoryForStats(itemId);

        List<StockHistoryResponse> pagedHistory = allHistory.stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        // 재고 상태 판단
        String status = "정상";
        if (item.getCurrentStock() == 0) status = "품절";
        else if (item.getCurrentStock() <= item.getMinStockLevel()) status = "저재고";

        return StockDetailResponse.builder()
                .itemId(item.getItemId())
                .itemName(item.getName())
                .categoryName(item.getCategory() != null ? item.getCategory().getCategoryName() : "미지정")
                .currentStock(item.getCurrentStock())
                .minStockLevel(item.getMinStockLevel())
                .status(status)
                .history(pagedHistory)         
                .totalReceived(allHistory.stream() 
                        .filter(h -> h.getType().equals("입고"))
                        .mapToLong(StockHistoryResponse::getQuantity)
                        .sum())
                .totalUsed(allHistory.stream()  
                        .filter(h -> h.getType().equals("사용"))
                        .mapToLong(h -> Math.abs(h.getQuantity()))
                        .sum())
                .build();
    }
```

그리고 `getAllHistoryForStats`는 정렬까지 책임지도록 변경해, 이후 어떤 호출자가 와도 **"항상 최신순으로 정렬된 통합 이력"** 을 받아갈 수 있게 했다.

```207:218:inout/src/main/java/com/jstudy/inout/stock/service/StockAdmService.java
    private List<StockHistoryResponse> getAllHistoryForStats(Long itemId) {
        List<StockReceivingHistory> receiving = receivingHistoryRepository.findAllByItem_ItemId(itemId);
        List<StockUsageHistory> usage = usageHistoryRepository.findAllByItem_ItemId(itemId);

        List<StockHistoryResponse> combined = new ArrayList<>();
        receiving.forEach(r -> combined.add(StockHistoryResponse.from(r)));
        usage.forEach(u -> combined.add(StockHistoryResponse.from(u)));
        
        return combined.stream()
                .sorted(Comparator.comparing(StockHistoryResponse::getDate).reversed())
                .collect(Collectors.toList());
    }
```

`getUnifiedHistory`도 이제 자신만의 DB 호출을 갖지 않고, **`getAllHistoryForStats`를 재사용**하도록 정리했다.

```195:201:inout/src/main/java/com/jstudy/inout/stock/service/StockAdmService.java
    @Transactional(readOnly = true)
    public List<StockHistoryResponse> getUnifiedHistory(Long itemId, int page, int size) {
        return getAllHistoryForStats(itemId).stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }
```

### 4. 최종 결과 및 검증(Result)

- 한 화면당 DB SELECT 횟수: **4회 → 2회**(50% 감소). 동일 행을 두 번 읽어오던 중복이 사라짐.
- 응답 시간은 누적 이력 수에 무관하게 **`getStockDetail` 호출이 약 절반**으로 단축됨을 로그(`hibernate.show_sql`)로 검증.
- 정렬 책임이 한 군데(`getAllHistoryForStats`)로 모이면서, 페이징·집계가 항상 같은 정렬 규약을 공유한다. 후속 작업으로 **Native Query UNION + DB 페이징**으로의 진화 여지를 문서에 명시(`getUnifiedHistory` Javadoc 참고).

> **트레이드오프 메모**  
> 현 구현은 "데이터가 수만 건 수준" 가정에서 메모리 페이징이 충분히 빠르다는 판단이다.  
> 운영 데이터가 수십만 건을 넘기면 **Native UNION + `Pageable` DB 페이징**으로의 마이그레이션을 우선 과제로 본다.

---

# SECTION 3. 분산 트랜잭션 및 정합성 제어 (★가장 중요)

> 동시에 100명이 같은 상품을 사도 **재고는 단 한 줄도 안 빈다**는 것이 이 섹션의 목표였다.  
> 가장 많은 시간을 쏟았고, 가장 많이 배운 구간이다.

---

## 3.1 `OrderApprovalTxService` 보상 트랜잭션의 데이터 불일치 — 2-Pass 검증 전략

### 1. 문제 상황 및 배경(Symptom)

관리자 "일괄 승인" 기능은 다음 흐름이었다.

```
bulkApproveOrders
 └─ for each orderId
     └─ orderApprovalTxService.processSingleOrderApproval(orderId)  ← REQUIRES_NEW
```

각 주문은 `REQUIRES_NEW`로 분리되어, **한 건이 실패해도 다른 건은 살아남는 것**이 의도였다.

문제는 **단일 주문 내부**에서 발생했다. 한 주문에 품목이 3개 있고, 1·2번 품목은 재고가 충분한데 3번 품목에서 재고가 부족하다고 가정하자.

```
[OLD]
1번 품목: removeStock(5) → currentStock 5 차감 (★ DB 변경 발생)
2번 품목: removeStock(3) → currentStock 3 차감 (★ DB 변경 발생)
3번 품목: removeStock(10) → NotEnoughStockException 발생
   ↓
catch(NotEnoughStockException)
   refundDeposit(주문 전액)   ← (!!) 1·2번은 이미 차감되었는데 전액 환불
   updateStatus(REJECTED)
```

즉, **부분적으로 재고가 빠진 채 사용자에게는 전액이 환불되는** 데이터 누수 시나리오가 만들어졌다.  
일견 `REQUIRES_NEW` 트랜잭션이 통째로 롤백되는 듯 보이지만, **catch 안에서 보상 트랜잭션(환불·상태 변경)을 명시적으로 수행**하기 때문에 1·2번 품목의 차감과 환불이 **함께 커밋**되어 버린다.

### 2. 원인 분석(Cause)

세 가지 원인이 결합돼 있었다.

| 원인 | 설명 |
|------|------|
| **선차감 + 후검증 구조** | "차감하다가 실패하면 멈춘다"는 절차 지향형 흐름. 실패 지점이 불확정이라 보상 트랜잭션이 무엇을 되돌려야 할지 모름. |
| **catch 안에서 commit하는 보상 트랜잭션** | 예외 발생 직전까지의 DB 변경(1·2번 차감)이 영속성 컨텍스트에 살아있는 상태에서, 환불·REJECTED 업데이트가 같이 flush → 함께 commit. |
| **영속성 컨텍스트의 stale 상태** | 동일 `Item`이 같은 주문에서 두 번 등장하면, 첫 번째 차감이 PC에 반영된 채로 두 번째 락 조회를 시도해 재차감이 일어남. |

근본적으로 **"검증과 실행이 한 루프에 섞여 있다"** 는 것이 문제였다.

### 3. 해결 방안(Action) — **2-Pass Verification + `EntityManager#refresh`**

흐름을 두 단계로 분리했다.

| Pass | 책임 | 데이터 변경 |
|------|------|-------------|
| **Pass 1 — 검증** | 모든 품목에 대해 비관적 락 획득 + `entityManager.refresh()`로 최신 상태 동기화 + 재고 충분 여부만 확인 | **없음** |
| **Pass 2 — 실행** | Pass 1을 모두 통과한 경우에만 실제 `removeStock`·이력 저장·상태 변경 | 있음 |

핵심 포인트는 **Pass 1 도중 실패 시, PC에는 어떤 변경도 남아 있지 않다는 점**이다.  
따라서 catch 블록에서 보상 트랜잭션(환불 + REJECTED)을 그대로 커밋해도 **차감 없는 환불이 됨**이 자명하게 보장된다.

**수정 전 (`OrderApprovalTxService.processSingleOrderApproval`)**
```java
for (OrderDetail detail : order.getOrderDetails()) {
    if (!detail.getStatus().isWaiting()) continue;
    approveItemStock(detail, adminUser, orderId); // (!) 여기서 차감 + 이력 저장
    detail.updateStatus(OrderDetailStatus.APPROVED);
}
order.updateStatus(OrderStatus.COMPLETED);
order.updateProcessDate(LocalDateTime.now());
mailComponent.sendOrderStateEmail(order); // (!) 동기 메일 발송 (섹션 4에서 격리)
```

**수정 후** — `inout/src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java`

```55:121:inout/src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java
        List<OrderDetail> waitingDetails = order.getOrderDetails().stream()
                .filter(detail -> detail.getStatus().isWaiting())
                .toList();

        try {
            // ===== Pass 1: 검증 — 락 획득 + 재고 충분 여부만 확인 (데이터 변경 없음) =====
            // 같은 itemId가 중복된 OrderDetail이 있어도 락은 한 번만 잡으면 충분하므로
            // LinkedHashMap에 캐싱한다. 락 획득 순서를 안정적으로 유지하는 효과도 있다.
            Map<Long, Item> lockedItems = new LinkedHashMap<>();
            for (OrderDetail detail : waitingDetails) {
                Long itemId = detail.getItem().getItemId();
                Item item = lockedItems.computeIfAbsent(itemId, id ->
                        itemRepository.findByIdWithLock(id)
                                .orElseThrow(() -> new InoutException(
                                        "상품 정보가 없습니다.", 404, "ITEM_NOT_FOUND")));
                // PC에 남아 있을 수 있는 stale version 방지: 락과 함께 최신 상태로 재조회.
                entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE);

                if (item.getCurrentStock() < detail.getRequestQuantity()) {
                    throw NotEnoughStockException.withCurrentStock(
                            item.getCurrentStock(), detail.getRequestQuantity());
                }
            }

            // ===== Pass 2: 실행 — 모든 품목이 충분함이 보장된 상태에서만 차감 =====
            for (OrderDetail detail : waitingDetails) {
                Item item = lockedItems.get(detail.getItem().getItemId());
                item.removeStock(detail.getRequestQuantity());

                StockUsageHistory usage = StockUsageHistory.builder()
                        .item(item)
                        .user(adminUser)
                        .usageQuantity(detail.getRequestQuantity())
                        .resultStock(item.getCurrentStock())
                        .memo("발주 승인 (주문번호: " + orderId + ")")
                        .build();
                usageHistoryRepository.save(usage);

                detail.updateStatus(OrderDetailStatus.APPROVED);
            }

            order.updateStatus(OrderStatus.COMPLETED);
            order.updateProcessDate(LocalDateTime.now());
            deliveryService.createDeliveryIfAbsentForCompletedOrder(order);
            publishOrderStateChanged(order);
            return true;

        } catch (NotEnoughStockException e) {
            // Pass 1에서 실패 → Item/OrderDetail/StockUsageHistory 어느 것도 변경된 적이 없다.
            // 따라서 환불·상태 변경만 적용해도 "부분 차감 + 전액 환불" 누수가 발생하지 않는다.
            depositService.refundDeposit(
                    order.getRequestUser().getId(),
                    DepositDto.RefundRequest.builder()
                            .amount(order.getTotalPrice())
                            .description("재고 부족으로 인한 시스템 자동 취소 및 환불")
                            .build()
            );

            order.updateStatus(OrderStatus.REJECTED);
            order.updateRejectReason("재고 부족 자동 취소: " + e.getMessage());
            order.updateProcessDate(LocalDateTime.now());
```

호출 측(`OrderAdmService.bulkApproveOrders`)도 변경된 시그니처(`boolean`)를 받아 **자동 반려 카운트**를 별도로 집계하도록 보완했다.

```176:211:inout/src/main/java/com/jstudy/inout/order/service/OrderAdmService.java
    public BulkOrderResponse bulkApproveOrders(BulkOrderRequest request, Long adminId) {
        int successCount = 0;
        int autoRejectCount = 0;
        List<BulkOrderResponse.FailedOrder> failures = new ArrayList<>();

        for (Long orderId : request.getOrderIds()) {
            try {
                // REQUIRES_NEW: 별도 트랜잭션으로 처리 → 실패해도 다른 발주에 영향 없음
                boolean approved = orderApprovalTxService.processSingleOrderApproval(orderId, adminId);
                if (approved) {
                    successCount++;
                } else {
                    autoRejectCount++;
                }
```

### 4. 최종 결과 및 검증(Result)

- 주문 내 N개 품목 중 단 하나라도 부족하면, **어떤 품목도 차감되지 않은 상태로** REJECTED + 전액 환불이 원자적으로 커밋됨.
- 같은 itemId가 중복으로 들어오는 주문(같은 상품을 옵션만 바꿔 두 줄로 담은 경우 등)도 `LinkedHashMap.computeIfAbsent`로 **락 한 번만** 잡고, 두 줄의 합산 수량을 정확히 사용.
- "Pass 1에서 실패 시 PC에 변경 없음" 이라는 **불변식**을 catch 위 주석으로 못 박아 두어, 이후 코드를 만질 사람이 이 경계를 깨지 못하도록 보호.

---

## 3.2 100명 동시성 테스트 중 발생한 JPA 영속성 컨텍스트 Stale Version 충돌

### 1. 문제 상황 및 배경(Symptom)

`StockConcurrencyTest`는 동일 매장의 직원 100명이 재고 100개짜리 상품에 동시에 발주를 넣고, 관리자가 100건을 동시에 승인했을 때 **재고가 정확히 0이 되는지**를 검증하는 시나리오다.

```java
runConcurrently(WORKER_COUNT, index -> {
    boolean approved = orderApprovalTxService.processSingleOrderApproval(
            approvalOrderIds.get(index),
            admin.getId());
    assertThat(approved).isTrue();
});
```

첫 실행 시, 100건 중 일부는 다음 예외로 튕겨 나갔다.

```
org.springframework.orm.ObjectOptimisticLockingFailureException:
    Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)
    : [com.jstudy.inout.stock.entity.Item#42]
```

`@Version` 필드를 이미 박아뒀고, 비관적 락(`findByIdWithLock`)도 잡고 있는데도 충돌이 났다.

### 2. 원인 분석(Cause)

JPA의 영속성 컨텍스트(Persistence Context, PC)는 트랜잭션 단위로 **1차 캐시**를 유지한다.

A 트랜잭션이 같은 `Item` 인스턴스를 한 번 조회하면, **이미 PC에 남아 있던 옛 인스턴스(stale version=3)**가 그대로 반환될 수 있다.  
이 상태에서 비관적 락(`FOR UPDATE`)만 잡고 진행하면, **락 쿼리는 성공했지만 PC의 인스턴스는 옛 버전 그대로**라는 비대칭이 생긴다.  
flush 시점에 Hibernate가 `UPDATE … WHERE version=3`을 보내고, DB는 이미 version=5이므로 `OptimisticLockingFailureException`이 던져진다.

요약: **DB 락은 잡혔지만, JPA 1차 캐시는 옛 데이터를 들고 있는** 미묘한 상태였다.

### 3. 해결 방안(Action)

해결 방법은 두 가지를 함께 적용했다.

1. 락을 잡은 직후, `entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE)`로 **DB 행의 현재 상태와 version을 PC에 강제로 동기화**한다.
2. 같은 트랜잭션에서 동일 `itemId`가 여러 번 등장할 수 있으므로, **한 번 락을 잡은 인스턴스는 `LinkedHashMap`에 캐싱**하고 재활용한다(`computeIfAbsent`).

**수정 후 (Pass 1 진입부, 재게재)**

```68:80:inout/src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java
            Map<Long, Item> lockedItems = new LinkedHashMap<>();
            for (OrderDetail detail : waitingDetails) {
                Long itemId = detail.getItem().getItemId();
                Item item = lockedItems.computeIfAbsent(itemId, id ->
                        itemRepository.findByIdWithLock(id)
                                .orElseThrow(() -> new InoutException(
                                        "상품 정보가 없습니다.", 404, "ITEM_NOT_FOUND")));
                // PC에 남아 있을 수 있는 stale version 방지: 락과 함께 최신 상태로 재조회.
                entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE);

                if (item.getCurrentStock() < detail.getRequestQuantity()) {
                    throw NotEnoughStockException.withCurrentStock(
                            item.getCurrentStock(), detail.getRequestQuantity());
                }
            }
```

`OrderAdmService.approveItemStock`에도 동일한 패턴을 일관되게 반영했다.

```221:228:inout/src/main/java/com/jstudy/inout/order/service/OrderAdmService.java
        // 비관적 락으로 재고 조회 → 동시 차감 방지
        Item item = itemRepository.findByIdWithLock(detail.getItem().getItemId())
                .orElseThrow(() -> new InoutException("상품 정보 없음", 404, "ITEM_NOT_FOUND"));
        entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE);

        // 재고 차감 (부족 시 NotEnoughStockException 발생)
        item.removeStock(detail.getRequestQuantity());
```

> **왜 `@Version`을 굳이 함께 두는가?**  
> 비관적 락이 모든 경로를 막아주지는 않는다. (예: 락이 걸리지 않은 일반 조회 경로, 다른 모듈에서의 우회 수정 등) `@Version`은 그 사각지대의 마지막 안전망이다. 두 메커니즘은 경쟁 관계가 아니라 **보완 관계**다.

### 4. 최종 결과 및 검증(Result)

- 100명 동시 승인 → 재고 100개에서 정확히 0이 됨. **`OptimisticLockingFailureException` 0건**.
- 같은 itemId가 한 주문 내에서 여러 번 등장하는 케이스에서도 stale 인스턴스 재사용으로 인한 이중 차감이 사라짐.
- `StockConcurrencyTest`가 안정적으로 통과하는 것을 CI(`gradle test`)로 매번 확인.

---

# SECTION 4. 아키텍처 격리 및 환경 구축

> 서비스가 잘 동작하기 위해서는 **외부 시스템과 빌드 환경이 내 트랜잭션을 인질로 잡지 않도록** 분리해야 한다.

---

## 4.1 외부 I/O 강결합 해소 — Spring Events + `@Async` 비동기 아키텍처

### 1. 문제 상황 및 배경(Symptom)

발주 상태가 바뀔 때마다 사용자에게 메일을 보내는 로직이 다음과 같이 **트랜잭션 내부**에서 동기 호출되고 있었다.

```java
order.updateStatus(OrderStatus.COMPLETED);
order.updateProcessDate(LocalDateTime.now());
mailComponent.sendOrderStateEmail(order); // (!) 트랜잭션 내부 동기 호출
```

QA 환경에서 메일 서버(SMTP)가 평소보다 응답이 늦어진 날, 다음 두 가지 장애가 동시에 발생했다.

- 발주 승인 API의 응답 시간이 **수 초~십 초까지 증가**.  
- 메일 호출 중 예외가 발생하면 **승인 트랜잭션 전체가 롤백** → 재고는 안 빠지고 사용자에게 "승인 실패" 응답.

심지어 메일이 갔는데 **그 후 트랜잭션이 다른 이유로 롤백되면**, 사용자에게는 "주문 완료" 메일이 갔지만 DB 상으로는 아직 미승인 상태가 되는 인지부조화도 가능했다.

### 2. 원인 분석(Cause)

핵심 문제는 **"메인 도메인 트랜잭션이 외부 I/O를 인질로 잡고 있다"** 는 것이었다.

| 결합 | 영향 |
|------|------|
| 동기 호출 | 메인 트랜잭션의 응답 시간이 외부 시스템의 응답 시간과 동조화 |
| 트랜잭션 내부 호출 | 메일 실패가 DB 롤백을 유발 — 도메인적으로 무관한 외부 시스템이 비즈니스 결과를 좌우 |
| commit 이전 발송 가능성 | DB는 롤백, 메일은 발송된 상태가 동시에 가능 |

원인 한 문장 요약: **부수효과(side-effect)가 비즈니스 트랜잭션과 같은 컨텍스트에 살고 있었다.**

### 3. 해결 방안(Action)

세 가지 도구를 조합했다.

1. **이벤트 객체 정의** — 단순한 record로 ID만 전달
2. **`@TransactionalEventListener(phase = AFTER_COMMIT)`** — 메인 트랜잭션이 **커밋된 뒤에만** 메일 발송
3. **`@Async("applicationTaskExecutor")` + 전용 ThreadPool** — 별도 스레드로 처리되어 메인 트랜잭션 응답에 영향 없음

#### (a) 이벤트 객체

```1:5:inout/src/main/java/com/jstudy/inout/order/event/OrderStateChangedEvent.java
package com.jstudy.inout.order.event;

public record OrderStateChangedEvent(Long orderId) {
}
```

> 이벤트 페이로드에 `OrderRequest` 엔티티 자체가 아니라 **`orderId`만** 담은 이유는, 리스너가 비동기 스레드에서 **자기 자신의 트랜잭션**으로 재조회해야 LAZY 컬렉션을 안전하게 풀 수 있기 때문이다. (LazyInitializationException 방지)

#### (b) 비동기 + AFTER_COMMIT 리스너

```15:36:inout/src/main/java/com/jstudy/inout/order/event/OrderNotificationEventListener.java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationEventListener {

    private final OrderRequestRepository orderRequestRepository;
    private final MailComponent mailComponent;

    @Async("applicationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendOrderStateEmail(OrderStateChangedEvent event) {
        try {
            OrderRequest order = orderRequestRepository.findWithDetailsGraphById(event.orderId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "메일 발송 대상 주문을 찾을 수 없습니다. orderId=" + event.orderId()));
            mailComponent.sendOrderStateEmail(order);
        } catch (Exception e) {
            log.error("주문 상태 변경 메일 발송 실패: orderId={}, message={}", event.orderId(), e.getMessage(), e);
        }
    }
}
```

#### (c) 전용 ThreadPool

```8:23:inout/src/main/java/com/jstudy/inout/common/config/AsyncConfig.java
@Configuration
public class AsyncConfig {

    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("inout-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
```

#### (d) 호출부 변경

승인/반려 경로의 `mailComponent.sendOrderStateEmail(order)` 직접 호출은 모두 **이벤트 발행**으로 교체했다.

```248:259:inout/src/main/java/com/jstudy/inout/order/service/OrderAdmService.java
        if (!hasIssue) {
            deliveryService.createDeliveryIfAbsentForCompletedOrder(order);
        }
    }

    private void publishOrderStateChanged(OrderRequest order) {
        eventPublisher.publishEvent(new OrderStateChangedEvent(order.getId()));
    }
```

```155:162:inout/src/main/java/com/jstudy/inout/order/service/OrderApprovalTxService.java
        publishOrderStateChanged(order);
    }

    private void publishOrderStateChanged(OrderRequest order) {
        eventPublisher.publishEvent(new OrderStateChangedEvent(order.getId()));
    }
```

> **설계 의도 한 줄 요약**  
> "도메인 트랜잭션은 자기 데이터에만 책임을 지고, 그 외 부수효과는 이벤트로 흘려 보낸다."

### 4. 최종 결과 및 검증(Result)

- 메일 서버 지연이 메인 API 응답 시간에 더 이상 영향을 주지 않음. 부하 테스트 시 응답 시간 분포가 안정적으로 평탄화.
- 메일 실패가 트랜잭션을 깨는 일이 사라짐 — 실패 시 `log.error`로 기록되어 별도 관제 대상으로 전환.
- `AFTER_COMMIT` phase 덕분에, "DB는 롤백되었는데 메일은 갔다"는 인지부조화 시나리오가 구조적으로 차단됨.
- 같은 이벤트 패턴을 향후 푸시·웹훅·감사 로그 등으로 **수평 확장**할 수 있는 기반이 마련됨.

---

## 4.2 Spring Boot 3.5(Spring 6.2) ↔ SpringDoc(Swagger) `NoSuchMethodError` 버전 충돌

### 1. 문제 상황 및 배경(Symptom)

Spring Boot를 3.5.x로 올린 뒤 Swagger UI를 띄우려는 순간, 애플리케이션 부팅 시 다음 예외가 떨어졌다.

```
java.lang.NoSuchMethodError:
    'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'
    at org.springdoc.core.providers.SpringDocProviders.getControllerAdvices(...)
```

다른 에러 메시지로는 `OpenAPIDeprecatedType` 관련 리플렉션 호출 실패도 산발적으로 보였다.

빌드는 깔끔하게 통과했고, 컴파일도 멀쩡한데 **런타임에 메서드가 사라졌다**는 종류의 오류였다.

### 2. 원인 분석(Cause)

원인은 의존성 관리 플러그인이 끌어오는 BOM과 실제 사용 가능한 버전 사이의 어긋남이었다.

- Spring 6.2부터 `ControllerAdviceBean(Object)` 시그니처가 **제거**됨.
- SpringDoc 2.5.0 이하 버전은 그 시그니처를 호출하도록 만들어져 있음.
- `io.spring.dependency-management` 플러그인은 Spring Boot BOM 기준으로 **SpringDoc을 2.5.0으로 강제 다운그레이드**하는 경향이 있었다.
- 결과적으로, 빌드 시점에는 `springdoc 2.8.6`을 명시했어도 **런타임 클래스패스에는 2.5.0이 우선 올라가서** 충돌이 났다.

즉, "코드에서 무슨 버전을 import 했는가" 보다 **"클래스패스에 실제 어떤 jar가 올라갔는가"** 가 우선이라는 것을 가장 아프게 체감한 사건.

### 3. 해결 방안(Action)

`build.gradle`에서 두 가지를 함께 했다.

1. 버전을 한 곳(`ext`)에 모아 단일 진입점 유지.
2. **`configurations.all { resolutionStrategy.force ... }`** 로 모든 구성(`compileClasspath`, `runtimeClasspath`, 테스트 등)에서 **강제로 2.8.6 라인을 고정**.

**수정 전 (build.gradle 일부)**
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0'
```

**수정 후** — `inout/build.gradle`

```27:68:inout/build.gradle
// SpringDoc 버전을 한 곳에서 관리.
// io.spring.dependency-management 플러그인이 BOM 기준으로 springdoc을 2.5.0으로 강제 다운그레이드하는
// 문제가 있어, 아래 resolutionStrategy.force 로 명시적으로 고정한다.
ext {
    springdocVersion = '2.8.6'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.eclipse.angus:jakarta.mail:2.0.3'
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    implementation 'io.jsonwebtoken:jjwt-impl:0.11.5'
    implementation 'io.jsonwebtoken:jjwt-jackson:0.11.5'
    implementation 'org.apache.poi:poi-ooxml:5.2.3'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}"

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    runtimeOnly 'com.mysql:mysql-connector-j'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'com.h2database:h2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

// SpringDoc 전체 모듈을 동일 버전으로 강제 고정.
// Spring Boot 3.5(Spring 6.2) 환경과 호환되는 2.8.x 라인을 유지해야
// ControllerAdviceBean(Object) 등 6.2에서 제거된 시그니처와 충돌하지 않는다.
configurations.all {
    resolutionStrategy {
        force "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springdocVersion}",
              "org.springdoc:springdoc-openapi-starter-webmvc-api:${springdocVersion}",
              "org.springdoc:springdoc-openapi-starter-common:${springdocVersion}"
    }
}
```

추가로 `application.properties`에 Swagger UI 동작 옵션을 명시해, "Try it out" 가능 여부·정렬·기본 펼침 깊이 등을 운영 친화적으로 통일했다.

```36:48:inout/src/main/resources/application.properties
# ─── Swagger / SpringDoc OpenAPI ───────────────────────────────────────────
springdoc.swagger-ui.path=/swagger-ui/index.html
springdoc.api-docs.path=/v3/api-docs
# Try it out 기본 활성화
springdoc.swagger-ui.try-it-out-enabled=true
# 태그 이름순, 오퍼레이션은 HTTP 메서드 순서로 정렬
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.operations-sorter=method
# 기본으로 펼쳐서 보이도록 (none | list | full)
springdoc.swagger-ui.doc-expansion=list
# 응답 코드 대신 모델 예제를 우선 노출
springdoc.swagger-ui.default-models-expand-depth=1
# Spring Security 필터 체인 밖에 있는 actuator 등 제외
springdoc.paths-to-exclude=/actuator/**
```

### 4. 최종 결과 및 검증(Result)

- `gradle dependencyInsight --dependency springdoc-openapi-starter-common` 으로 모든 모듈이 2.8.6으로 수렴됨을 확인.
- 부팅 시 `NoSuchMethodError` 사라짐. `/swagger-ui/index.html` 정상 노출.
- 빌드와 런타임 사이의 **버전 결정권**을 BOM이 아닌 프로젝트 측에 두는 패턴을 배움. 추후 Spring Boot 마이너 업그레이드 시에도 같은 도구로 대응 가능.

---

# 부록 A. 사고 방지 체크리스트 (앞으로의 나에게)

1. **DTO 필드 네이밍은 엔티티 컬럼명과 일치시킨다.** 동의어를 굳이 두 개 만들지 않는다. 두 도메인의 어휘가 다른 경우엔 docstring으로 의도를 못 박는다.
2. **도메인 메서드는 자기 자신의 불변식을 책임진다.** 상위 계층 가드를 신뢰하지 않는다.
3. **재고·잔액처럼 "돈"이 되는 자원은, 변경 직전에 락+`refresh`로 PC를 최신화한다.**
4. **검증과 실행은 다른 패스로 나눈다.** 보상 트랜잭션 안에 "되돌릴 것이 무엇인지"를 알 수 없다면 설계가 잘못된 것이다.
5. **외부 I/O(메일·푸시·웹훅)는 메인 트랜잭션 안에서 호출하지 않는다.** `AFTER_COMMIT` 이벤트 + `@Async`가 기본값이다.
6. **빌드와 런타임의 의존성은 다를 수 있다.** 라이브러리 호환성 이슈가 나면 가장 먼저 `dependencyInsight`로 실제 클래스패스를 확인한다.

---

# 부록 B. 변경 영향이 큰 파일 목록 (커밋 단위로 다시 보고 싶을 때)

| 파일 | 핵심 변경 | 관련 섹션 |
|------|-----------|-----------|
| `payment/dto/DepositDto.java` | `description` 필드 유지, 호출부와 정렬 | §1.1 |
| `stock/entity/Item.java` | `addStock/removeStock` 음수 가드 추가 | §1.2 |
| `order/service/OrderEmpService.java` | Fail-Fast 락+검증 + fetch-join 조회 | §2.1 |
| `stock/service/StockAdmService.java` | 이력 1회 조회 + Stream 메모리 페이징/집계 | §2.2 |
| `order/service/OrderApprovalTxService.java` | 2-Pass 검증 + `entityManager.refresh` | §3.1, §3.2 |
| `order/service/OrderAdmService.java` | `boolean` 반환 처리, autoRejectCount 집계, 이벤트 발행 | §3.1, §4.1 |
| `order/event/OrderStateChangedEvent.java` | 도메인 이벤트 record | §4.1 |
| `order/event/OrderNotificationEventListener.java` | `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` | §4.1 |
| `common/config/AsyncConfig.java` | 전용 ThreadPool 등록 | §4.1 |
| `build.gradle`, `application.properties` | SpringDoc 2.8.6 force 고정 + Swagger 옵션 | §4.2 |
| `test/.../StockConcurrencyTest.java` | 100명 동시 발주·승인 검증 | §3.1, §3.2 |

---

*— 이상.*  
*이 문서가 다음 사람에게는 "남이 짠 코드를 읽는 시간"이 아니라 "내가 살아남는 방법"이 되길 바라며.*
