# INOUT 프로젝트 AS-IS 상세 스토리보드

- **작성 기준일**: 2026-07-06
- **작성 방식**: 실제 구현된 백엔드 소스코드(Controller/DTO/Entity/Service)와 프론트엔드 소스코드(React `src/pages`, `src/api`)를 전수 분석하여 작성한 **코드 기반 AS-IS 명세서**입니다.
- **원본 대비 차이**: 기존 `스토리보드.pptx`의 IA 구조·`[화면 설명]-[데이터 연동]-[링크 설명]` 포맷을 그대로 계승하되, 아래 원칙을 적용했습니다.
  - 코드에는 있으나 원본 스토리보드에 없던 기능 → 섹션 번호 뒤에 **`(NEW)`** 표기 후 신규 섹션으로 추가
  - 원본 스토리보드에는 있으나 현재 코드에 구현되어 있지 않은 기능 → **`[미구현]`** 배너로 명시
  - 원본과 실제 구현이 다른 경우(정책/로직 변경) → **`⚠️ 구현 노트`** 로 별도 표기
- **공통 응답 포맷**: 모든 REST API는 아래 `ResponseMessage` 래퍼로 응답합니다.

```json
{
  "header": { "result": true, "resultCode": "SUCCESS", "message": "설명 메시지", "status": 200 },
  "body": { }
}
```

- **공통 인증/인가**: JWT Bearer 토큰(Stateless). `SecurityConfig` 기준 URL 단위 1차 필터링 + 컨트롤러 `@PreAuthorize` 2차 필터링(더 좁은 조건이 최종 실효 권한).

| URL 패턴 | 필요 권한 |
|---|---|
| `/api/admin/**` | ROLE_ADMIN |
| `/api/emp/**`, `/stock/emp/**`, `/order/emp/**` | ROLE_EMPLOYEE 또는 ROLE_ADMIN |
| `/api/inquiry/**` | ROLE_EMPLOYEE 또는 ROLE_ADMIN |
| `/api/payment/**`, `/api/deposit/**` | ROLE_EMPLOYEE 또는 ROLE_ADMIN |
| `/api/dashboard/**` | 로그인 사용자(권한 무관) |
| `/api/user/login`, `/register`, `/refresh`, `/find`, `/resetPassword`, `/public/**` | 비로그인 허용 |
| `/oauth2/**`, `/login/oauth2/**` | 비로그인 허용 |

---

## 0. 표지 / 목차 (INDEX)

### 0.1 표지
재고 프로젝트 스토리보드 → **INOUT — B2B 발주·재고 관리 시스템 AS-IS 스토리보드**

### 0.2 INDEX (목차)

1. Information Architecture
2. 공통(Common) — 레이아웃/내비게이션
3. 로그인 / 회원가입 / 계정찾기
4. 대시보드
5. 재고 관리
6. 발주 관리
7. 배송 관리 `(NEW)`
8. 예치금 관리 `(NEW)`
9. 직원(회원) 관리
10. 문의사항 관리
11. 연차(휴가) 관리 `(NEW)`
12. AI 자동발주/인사이트 `(NEW)`
13. 원본 스토리보드 대비 변경/미구현 사항 총정리

---

## 1. Information Architecture (IA)

프론트엔드는 Thymeleaf가 아닌 **React 19 SPA**(`react-router-dom` v7)로 구현되어 있으며, 로그인 후 공통 `Layout`(사이드바+헤더) 하위에 역할별 라우트가 렌더링됩니다. (`frontend/src/App.jsx`, `frontend/src/components/layout/Layout.jsx`)

### 1.1 IA — 직원(EMPLOYEE)

```
Home(/emp/dashboard)
 ├─ 대시보드 (/emp/dashboard)
 ├─ 재고 조회 (/emp/stocks)
 ├─ 재고 사용 처리 (/emp/stock-use)
 ├─ 발주 내역 (/emp/orders) → 상세(/emp/orders/:orderId) → 결제(/emp/payment/:orderId)
 ├─ 배송 현황 (/emp/delivery) (NEW)
 ├─ 연차 신청 (/emp/vacation) → 신청 등록(/emp/vacation/new) → 상세(/emp/vacation/:leaveId) (NEW)
 ├─ 장바구니 (/emp/cart)
 ├─ 문의 사항 (/emp/inquiries) → 작성(/emp/inquiries/new) → 상세(/emp/inquiries/:inquiryId)
 ├─ 나의 예치금 (/emp/deposit) (NEW)
 └─ 내 정보 (/emp/profile)
```

### 1.2 IA — 관리자(ADMIN)

```
Home(/admin/dashboard)
 ├─ 대시보드 (/admin/dashboard) — AI 운영 인사이트 포함 (NEW)
 ├─ 발주 관리 (/admin/orders) → 상세처리(/admin/orders/:orderId)
 ├─ 배송 관리 (/admin/delivery) (NEW)
 ├─ 재고 관리 (/admin/stocks) → 신규등록(/admin/stocks/new) → 상세(/admin/stocks/:itemId)
 ├─ 직원 관리 (/admin/users) → 상세(/admin/users/:userId)
 ├─ 문의 사항 (/admin/inquiries) → 상세(/admin/inquiries/:inquiryId)
 ├─ 예치금 관리 (/admin/deposit) (NEW)
 ├─ 연차 관리 (/admin/vacation) (NEW)
 └─ 내 정보 (/emp/profile 공유)
```

> `⚠️ 구현 노트`: 원본 스토리보드의 로그인/회원가입/아이디찾기/비밀번호찾기 화면은 `Layout` 밖(비로그인) 영역이며, IA 트리에는 별도로 표기하지 않고 3장에서 다룹니다.

---

## 2. 공통(Common)

### 2.1 사이드바 내비게이션 + 헤더/로그아웃 (전체 페이지 공통)
*소스: `frontend/src/components/layout/Layout.jsx`*

**[화면 설명]**
- 좌측 고정 사이드바(폭 256px, 다크 네이비): 로고 `INOUT` + 서브카피 `B2B 발주·재고 시스템`
- 역할별 메뉴 자동 전환:
  - **관리자**: 대시보드 / 발주 관리 / 배송 관리 / 재고 관리 / 직원 관리 / 문의 사항 / 예치금 관리 / 연차 관리
  - **직원**: 대시보드 / 재고 조회 / 재고 사용 / 발주 내역 / 배송 현황 / 연차 신청 / 장바구니 / 문의 사항 / 내 정보 / 나의 예치금
- 사이드바 하단: 사용자 아바타(이니셜)·이메일·역할(ADMIN/EMPLOYEE) 배지 + 로그아웃 아이콘 버튼
- 상단 Topbar: 현재 페이지 제목(`PAGE_TITLE_MAP` 매핑), ADMIN 배지, 직원 전용 장바구니 아이콘(빨간 알림 뱃지), 사용자 정보, `로그아웃` 버튼

**[데이터 연동]**
- 별도 API 호출 없이 `localStorage`의 `accessToken`(JWT) payload를 클라이언트에서 직접 디코딩(`atob`)하여 `sub/email`, `roles`(`ADMIN` 포함 여부)를 판별
- 로그아웃: `POST /api/user/logout` (인증 필요) 호출 후 `accessToken`/`refreshToken` 로컬 삭제

**[링크 설명]**
- 사이드바 각 메뉴 클릭 → 해당 라우트로 SPA 네비게이션(페이지 새로고침 없음)
- 로그아웃 버튼 클릭 → `confirm('로그아웃 하시겠습니까?')` → 확인 시 로그아웃 API 호출 후 `/login`으로 이동
- 직원 화면의 장바구니 아이콘 클릭 → `/emp/cart` 이동

> `[미구현]` 원본 스토리보드 2.1의 하단 **공통 Footer**(공지사항 / 이용약관 / 개인정보취급방침 링크, 회사 주소·연락처)는 현재 SPA Layout에 대응 화면이 없습니다.

---

## 3. 로그인 / 회원가입 / 계정찾기

### 3.1 일반 로그인
*소스: `LoginPage.jsx`, `AuthLoginController.java`*

**[화면 설명]**
- 타이틀 `INOUT SYSTEM` / `재고 및 주문 관리 시스템`
- 입력 필드: 통합계정(이메일), 비밀번호(마스킹) + `자동 로그인` 체크박스
- 버튼: `로그인`, 소셜 로그인 3종(`카카오로 시작하기`, `Google로 시작하기`, `네이버로 시작하기`), `이메일로 가입하기`
- 링크: `아이디 / 비밀번호 찾기`
- 인라인 유효성 에러: 이메일/비밀번호 미입력 시 즉시 표시

**[데이터 연동]**
- API: `POST /api/user/login` (`permitAll`)
  - 요청: `{ "email": "String(@NotBlank)", "password": "String(@NotBlank)" }`
  - 응답: `{ "accessToken": "JWT(1h)", "refreshToken": "JWT(7d, Redis 저장)", "role": "ROLE_EMPLOYEE|ROLE_ADMIN" }`
  - 예외: 403 `ACCOUNT_LOCKED`("계정이 잠겼습니다...") / 401 `UNAUTHORIZED`("이메일 또는 비밀번호가 잘못되었거나, 5회 이상 실패하여 계정이 잠겼습니다.")
- 로그인 5회 연속 실패 시 계정 자동 잠금(`User.increaseFailedAttempt()`), 성공 시 실패 카운트 초기화

**[링크 설명]**
- `로그인` 성공 → JWT의 role이 ADMIN 포함 시 `/admin/dashboard`, 아니면 `/emp/dashboard`
- `이메일로 가입하기` → `/register`
- `아이디 / 비밀번호 찾기` → `/find-account`

---

### 3.2 소셜 로그인(OAuth2) `(NEW)`
*소스: `SecurityConfig.java`, `CustomOAuth2UserService.java`, `OAuth2AuthenticationSuccessHandler.java`, `OAuthAttributes.java`*

**[화면 설명]**
- LoginPage의 `Google로 시작하기` / `카카오로 시작하기` / `네이버로 시작하기` 버튼

**[데이터 연동]**
- 시작 URI: `GET /oauth2/authorization/{google|kakao|naver}`
- 콜백 URI(서버↔소셜사): `/login/oauth2/code/{provider}` (permitAll)
- 플랫폼별 파싱(`OAuthAttributes`): Google(`sub`,`name`,`email`) / Kakao(`id`,`kakao_account.email`,`profile.nickname`) / Naver(`response.id`,`response.name`,`response.email`)
- 이메일 없는 계정 → `OAuth2AuthenticationException`("이메일 정보를 제공받지 못했습니다...")
- 신규 사용자는 자동 회원가입 + `ROLE_EMPLOYEE` 부여, 기존 사용자는 소셜 프로필 갱신(`updateSocialProfile`)
- 성공 시 JWT 발급 후 프론트엔드로 302 리다이렉트: `{FRONTEND_URL}/oauth2/callback?accessToken=...&refreshToken=...&role=...`

**[링크 설명]**
- 소셜 로그인 성공 → `/oauth2/callback`에서 쿼리 파라미터 파싱 후 토큰 저장 → 대시보드 이동

> `⚠️ 구현 노트`: 백엔드 OAuth2 3사 연동은 완전히 구현되어 있으나, 프론트 `LoginPage`의 `카카오로 시작하기` 버튼은 현재 `"카카오 로그인은 현재 준비 중입니다."` 안내 Toast만 노출하도록 되어 있어 **일부 소셜 버튼의 프론트 연동은 미완료** 상태입니다.

---

### 3.3 회원가입
*소스: `RegisterPage.jsx`, `UserController.java`*

**[화면 설명]**
- 입력: Email, Password, Confirm, Name, Phone, Store(Select), Birthday
- `중복확인` 버튼(이메일) → 성공 시 `확인완료`로 전환
- `가입하기` 버튼, `로그인으로 돌아가기` 링크

**[데이터 연동]**
- 이메일 중복확인: `GET /api/user/public/check-email?email=...` — 중복 시 400 "이미 사용 중인 이메일입니다."
- 매장 목록: `GET /api/user/public/stores` (Redis 1시간 캐시) — `[{id, name}]`
- 회원가입: `POST /api/user/register`
  - 요청(`UserInput`): `email(@Email,@NotBlank)`, `name(@NotBlank)`, `password(@NotBlank,@Size(min=4))`, `confirmPassword(@NotBlank)`, `phone(@NotBlank,@Size(max=20))`, `storeId(@NotNull)`, `birthday(@NotNull)`
  - 응답: `{ "redirectUrl": "/user/login" }`
  - 예외: 400 `DUPLICATE_EMAIL`, 404 `STORE_NOT_FOUND`, 400(빈 코드) "기본 권한 정보를 찾을 수 없습니다."

**[링크 설명]**
- `가입하기` 성공 → alert("회원가입이 완료되었습니다. 로그인해 주세요.") 후 `/login`
- `로그인으로 돌아가기` → `/login`

> `⚠️ 구현 노트`: `confirmPassword`는 DTO에 존재하나 서비스 로직에서 서버 측 일치 검증을 하지 않고 **프론트엔드 자체 검증**에만 의존합니다.
> `[미구현]` 원본 스토리보드의 **이용약관 동의 체크박스**(D08) 및 회원가입 버튼 활성화 조건으로서의 약관 동의는 현재 `RegisterPage`에 존재하지 않습니다.

---

### 3.4 아이디 찾기 / 비밀번호 찾기(초기화 메일 발송)
*소스: `FindAccountPage.jsx`, `UserController.java`*

**[화면 설명]**
- 탭 전환형 단일 페이지: `아이디 찾기` / `비밀번호 찾기`
- 아이디 찾기: Name, Phone 입력 → 결과 인라인 노출(팝업이 아닌 같은 화면 내 표시)
- 비밀번호 찾기: Email, Name, Phone 입력 → `초기화 메일 전송` → 완료 안내 인라인 표시

**[데이터 연동]**
- 아이디 찾기: `POST /api/user/find` — 요청 `{name, phone}` (email 필드는 DTO에 있으나 **서버 로직 미사용**) → 응답 `UserResponse{id,email,name,phone}` / 예외: 404 "입력하신 정보와 일치하는 이메일을 찾을 수 없습니다."
- 비밀번호 초기화 메일: `POST /api/user/public/password/reset`
  - 요청(`UserPasswordResetInput`): `email(@Email,@NotBlank)`, `name(@NotBlank)`, `phone(@NotBlank)`
  - 비즈니스: 일치 시 UUID `resetKey` 생성(30분 유효) + 메일 발송
  - 실패: 400 "사용자 정보를 찾을 수 없습니다."

**[링크 설명]**
- `로그인 하러가기` / `로그인 화면으로` / `← 로그인으로 돌아가기` → `/login`

> `⚠️ 구현 노트`: 원본 스토리보드의 "아이디 찾기 - 결과 페이지"(3.2, 마스킹된 아이디 노출용 별도 풀페이지)는 별도 라우트가 아니라 `FindAccountPage` 내 인라인 결과 영역으로 통합 구현되었습니다. 또한 현재 API는 마스킹 처리(`masked_user_id`) 없이 이메일 그대로 반환합니다.

---

### 3.5 비밀번호 재설정 (이메일 링크 접속 후)
*소스: `ResetPasswordPage.jsx`, `UserController.java`*

**[화면 설명]**
- URL 쿼리 `?key=` 로 진입 · New Password / Confirm Password 입력 · 실시간 일치 여부 안내(`✓ 비밀번호가 일치합니다.`) · `비밀번호 변경하기` 버튼

**[데이터 연동]**
- 링크 유효성 확인: `GET /api/user/public/password/reset/check?key=...` — 예외: 400 `INVALID_LINK` / `EXPIRED_LINK`
- 재설정 완료: `POST /api/user/public/resetPassword?resetKey=...&newPassword=...&confirmPassword=...`
  - 응답: `{ "redirectUrl": "/user/login" }`
  - 예외: 비밀번호 불일치(400), `InoutException` 400 "유효하지 않은 재설정 링크입니다." / "링크 유효 시간(30분)이 만료되었습니다."

**[링크 설명]**
- 변경 성공 → alert 후 `/login`

---

## 4. 대시보드

### 4.1 대시보드 (직원)
*소스: `DashboardEmpPage.jsx`, `DashboardEmpController.java`*

**[화면 설명]**
- KPI 카드: 나의 예치금 잔액 / 장바구니 상품 수 / 진행 중인 발주 / 금일 재고 사용
- `우리 매장 재고 상태` 진행바: 주문 가능 / 품절 임박 / 본사 품절
- `내 발주 진행 현황`: 진행 중(승인/배송) / 처리 완료 / 반려됨
- `내 매장 알림 피드`, 바로가기 버튼(장바구니/발주 내역/문의 사항/예치금 내역)

**[데이터 연동]**
- API: `GET /api/emp/dashboard/summary` (EMPLOYEE, ADMIN)
- 응답(`DashboardEmpResponse`): `userName`, `storeName`, `depositBalance`, `cartItemCount`, `inProgressOrderCount`, `totalOrderCount`, `completedOrderCount`, `rejectedOrderCount`, `todayStockUseCount`, `totalActiveStockCount`, `normalStockCount`, `lowStockCount`, `outOfStockCount`, `recentActivities[]`

**[링크 설명]**
- KPI/바로가기 클릭 → `/emp/deposit`, `/emp/cart`, `/emp/orders`, `/emp/stocks`, `/emp/inquiries`

---

### 4.2 대시보드 (관리자)
*소스: `DashboardPage.jsx`, `DashboardController.java`*

**[화면 설명]**
- KPI: 금일 신규 발주 / 금일 주문액 / 배송 중 / 배송 완료
- 패널: 재고 상태 분포(정상/저재고/품절), 발주 처리 현황(승인완료/처리중/반려/미결제대기/금일입고/금일출고), 당일 배송 현황(완료/중/준비/완료율)
- `실시간 알림 피드`, 미읽음 문의 배너, 바로가기(발주/재고/회원 관리/API 문서)

**[데이터 연동]**
- API: `GET /api/dashboard/summary` (로그인 사용자 전체 허용, Redis 5분 캐시 key=`admin`)
- 응답(`DashboardSummaryResponse`): `userName`, `storeName`, `todayNewOrderCount`, `lowStockCount`, `todayOrderAmount`, `pendingDeliveryCount`, `shippingDeliveryCount`, `completedDeliveryCount`, `normalStockCount`, `outOfStockCount`, `totalActiveStockCount`, `pendingOrderCount`, `completedOrderCount`, `rejectedOrderCount`, `totalOrderCount`, `todayInCount`, `todayOutCount`, `unreadInquiryCount`, `recentActivities[]`

**[링크 설명]**
- 패널/바로가기 클릭 → `/admin/orders`, `/admin/delivery`, `/admin/stocks`, `/admin/users`, Swagger(새 탭)

---

### 4.3 AI 운영 인사이트 `(NEW)`
*소스: `DashboardPage.jsx`(패널), `AiInsightController.java`, `AiInsightService.java`*

**[화면 설명]**
- 관리자 대시보드 내 `AI 운영 인사이트 분석` 패널, `GEMINI` 배지, `AI 분석 시작`/`재분석` 버튼

**[데이터 연동]**
- API: `GET /api/admin/ai/insight` (ROLE_ADMIN, `@Async`, 컨트롤러 35초 타임아웃)
- 응답(`AiInsightResponse`): `report`(Gemini 생성 자연어 리포트), `generatedAt`("yyyy년 MM월 dd일 HH:mm"), `model`("gemini-2.5-flash")
- 최근 6개월 발주 트렌드·재고 현황·매장별 발주 빈도를 DB에서 집계해 프롬프트 구성 후 Gemini 호출
- 예외: 503 `AI_NOT_CONFIGURED`/`AI_API_ERROR`, 500 `AI_INVALID_RESPONSE`/`AI_PARSE_ERROR`/`AI_SERVICE_ERROR`, 504 `AI_TIMEOUT`(35초)

**[링크 설명]**
- 버튼 클릭 시에만 온디맨드 호출(자동 조회 아님), 실패 시 패널 내 `분석 실패` 인라인 메시지

---

## 5. 재고 관리

### 5.1 재고 조회 (직원)
*소스: `StockEmpPage.jsx`, `StockEmpController.java`*

**[화면 설명]**
- 검색창(상품명), 테이블: 상품명/카테고리/단가/본사 재고/장바구니 담기 버튼
- 상세 모달: 단가, 본사 재고, 주문 수량 입력, 총 주문 금액, `장바구니 담기`

**[데이터 연동]**
- 목록: `GET /api/emp/stocks?name=&page=&size=&sort=name,ASC` (EMPLOYEE, ADMIN) — `Page<ItemResponse>`(itemId, categoryName, name, unitPrice, currentStock, status[정상/재고부족/품절], unitDescription), `deleted=false`만 조회
- 상세: `GET /api/emp/stocks/{itemId}` — `StockUserDetailResponse` / 예외: 404 `ITEM_NOT_FOUND`
- 장바구니 담기: `POST /api/emp/carts` — 요청 `{itemId(@NotNull), quantity(@Min(1))}`

**[링크 설명]**
- `+ 담기` → 모달 오픈 → `장바구니 담기` 클릭 시 장바구니 API 호출, 성공 Toast `장바구니에 추가되었습니다.`

---

### 5.2 재고 사용 처리 (직원)
*소스: `StockUseEmpPage.jsx`, `StockEmpController.java`*

**[화면 설명]**
- 카테고리 셀렉트(`전체 카테고리`), 상품명 검색
- 테이블: No/물품명/카테고리/현재 재고/사용 수량 입력+`사용처리` 버튼, 재고 0인 상품은 `입고 요청 필요` 뱃지

**[데이터 연동]**
- API: `POST /api/emp/stocks/use` (ROLE_EMPLOYEE 전용, ADMIN 불가)
  - 요청(`StockUseRequest`): `itemId(@NotNull)`, `quantity(@Min(1))`, `memo`
  - 비즈니스: 비관적 락 조회 → `Item.removeStock()` → `StockUsageHistory` 저장
  - 예외: 404 `ITEM_NOT_FOUND`, 400 `NOT_ENOUGH_STOCK`("재고가 부족합니다. (현재: %d, 요청: %d)")

**[링크 설명]**
- `사용처리` 클릭 → confirm("[상품명] N개를 사용 처리하시겠습니까?") → 성공 시 목록 갱신(새로고침 없이 재조회)

---

### 5.3 재고 목록 (관리자)
*소스: `StockPage.jsx`, `StockAdmController.java`*

**[화면 설명]**
- 요약 카드: 총 활성 상품/정상 재고/저재고 경고/품절
- 저재고 배너, 상품명 검색, `비활성 상품 포함` 체크박스, `신규 상품 등록` 버튼
- 테이블: 상품명/카테고리/단가/현재 재고/안전 재고/상태(정상/저재고/품절/비활성)

**[데이터 연동]**
- 목록: `GET /api/admin/stocks?name=&deleted=false&page=&size=&sort=itemId,DESC` (ADMIN) — `Page<StockAdminResponse>`
- 저재고 알림: `GET /api/admin/stocks/alerts/low-stock` (Redis 캐시) — `currentStock ≤ minStockLevel` 목록

**[링크 설명]**
- 상품명 클릭 → `/admin/stocks/{itemId}`
- `신규 상품 등록` → `/admin/stocks/new`

---

### 5.4 상품 등록 (관리자)
*소스: `StockRegisterAdmPage.jsx`, `StockAdmController.java`*

**[화면 설명]**
- 대표 이미지 첨부(선택), 상품명*, 카테고리*(커피/음료(1), 베이커리/디저트(2), 포장재/소모품(3)), 단가(원)*, 안전 재고, 단위 설명, 상세 설명
- 버튼: `취소`, `상품 등록`

**[데이터 연동]**
- 이미지 업로드: `POST /api/admin/images/upload` (multipart, ADMIN) → 응답 `String`(이미지 URL) / 예외: 400 `FILE_EMPTY`, 500 `FILE_UPLOAD_ERROR`
- 등록: `POST /api/admin/stocks`
  - 요청(`StockRegister`): `name(@NotBlank)`, `categoryId(@NotNull)`, `unitPrice(@NotNull,@Min(0))`, `minStockLevel(@Min(0))`, `unitDescription`, `description`, `imageUrl`
  - 응답: `Long`(신규 itemId), 메시지 "상품 등록이 완료되었습니다."
  - 예외: 404 `CATEGORY_NOT_FOUND`, 400 `DUPLICATE_ITEM_NAME`

**[링크 설명]**
- 등록/취소 성공 시 `/admin/stocks` 이동

> `⚠️ 구현 노트`: 반환되는 이미지 URL은 `/uploads/...` 형태이나, `WebConfig`의 정적 리소스 매핑은 `/api/uploads/**`만 되어 있어 **이미지 접근 경로가 실제와 불일치할 수 있는 잠재 버그**가 있습니다.

---

### 5.5 재고 상세 / 통합 이력 / 재고 실사 (관리자)
*소스: `StockDetailAdmPage.jsx`, `StockAdmController.java`*

**[화면 설명]**
- 요약: 현재 상태/현재 재고/안전 재고 기준
- `재고 실사 입력`: 실사 후 실제 수량, 조정 사유* → `실사 반영하기`
- 통계: 누적 입고량/누적 사용량
- 이력 테이블: 구분(입고/사용)/날짜/수량/처리 후 재고/담당자

**[데이터 연동]**
- 상세: `GET /api/admin/stocks/{itemId}?page=&size=20` — `StockDetailResponse`(itemName, categoryName, currentStock, minStockLevel, status, imageUrl, history[], totalReceived, totalUsed) / 예외: 404 `ITEM_NOT_FOUND`
- 이력(별도): `GET /api/admin/stocks/{itemId}/history?page=&size=20` — `List<StockHistoryResponse>`(페이징 메타 없음, 메모리 slice)
- 실사 조정: `PATCH /api/admin/stocks/{itemId}/adjust`
  - 요청(`StockAdjustRequest`): `adjustedQuantity(@NotNull,@Min(0))`, `reason(@NotBlank)`
  - 비즈니스: 비관적 락 → `diff=조정수량-현재재고` → 0이면 무동작, 양수면 입고이력(`[재고조정] {사유}`), 음수면 사용이력+차감
  - 예외: 404 `ITEM_NOT_FOUND`/`ADMIN_NOT_FOUND`, 400 `NOT_ENOUGH_STOCK`
- 입고 처리(별도 백엔드 API, 아래 참고): `POST /api/admin/stocks/receive`

**[링크 설명]**
- `실사 반영하기` 클릭(diff=0이면 비활성) → 성공 Toast "재고 실사가 반영되었습니다."
- 뒤로가기 → `/admin/stocks`

> `[미구현]` 원본 스토리보드 5.8 "입고 처리" 및 5.9 "전체 입고 이력 보기" 전용 화면은 프론트엔드에 존재하지 않습니다. 백엔드 API `POST /api/admin/stocks/receive`(요청: `itemId`, `quantity(@Min(1))`, `memo` / 예외: 404 `ITEM_NOT_FOUND`,`USER_NOT_FOUND`, 409 `CONCURRENCY_ERROR`)는 **구현되어 있으나 이를 호출하는 화면이 없어, 현재는 "재고 실사(조정)" 화면으로 기능이 대체·통합**되었습니다. 마찬가지로 "전체 입고 이력"(전체 상품 통합 조회) API/화면도 없고, 상품별 통합 이력(`/{itemId}/history`)만 제공됩니다.

---

## 6. 발주 관리

### 6.1 장바구니 (직원)
*소스: `CartEmpPage.jsx`, `CartEmpController.java`*

**[화면 설명]**
- 상품별 수량 `−`/`+` 조절, `삭제`, 총 주문 수량/총 결제 예상 금액, `전체 발주 결제하기` 버튼

**[데이터 연동]**
- 조회: `GET /api/emp/carts` — `CartResponse{items[](cartId=실제로는 cartDetailId, itemName, quantity, unitPrice, subTotal), totalQuantity, totalPrice}`
- 수량변경: `PATCH /api/emp/carts/{cartDetailId}/quantity` — 요청 `{quantity(@NotNull,@Min(1))}` / 예외: 404, 403 `FORBIDDEN`
- 선택삭제: `DELETE /api/emp/carts/items` — 요청 `List<Long>`(cartDetailIds)
- 발주 신청(주문서 생성): `POST /api/emp/orders`

**[링크 설명]**
- `전체 발주 결제하기` → 발주 신청 API 성공 시 `/emp/payment/{orderId}` 이동

> `⚠️ 구현 노트`: `CartResponse.CartItemResponse.cartId` 필드명은 실제로는 `cartDetailId` 값을 담고 있어 프론트 연동 시 혼동 주의가 필요합니다.

---

### 6.2 발주 신청(미리보기 포함) (직원)
*소스: `CartEmpPage.jsx`, `OrderEmpController.java`*

**[화면 설명]**
- 장바구니에서 선택된 품목 확인 후 결제 화면으로 자동 전환(별도 "발주 등록" 폼 화면 없이 장바구니→결제 흐름으로 단순화됨)

**[데이터 연동]**
- 미리보기: `POST /api/emp/orders/preview` — 요청(`OrderCreateRequest`): `cartDetailIds(@NotEmpty)`, `memo`, `receiverName(@Size(max=100))`, `receiverPhone(@Size(max=30))`, `destinationAddress(@Size(max=255))` → 응답(`OrderPreResponse`): storeName, employeeName, storeAddress, items[], totalQuantity, totalPrice (DB 저장 없음)
- 실제 신청: `POST /api/emp/orders` — 동일 요청, 응답 `Long`(orderRequestId), 상태 `REQUESTED`(결제 대기)로 생성, 재고는 승인 시점에만 차감(신청 시 검증만)
- 예외: 400 `EMPTY_ORDER`/`EMPTY_SELECTION`, 403 `FORBIDDEN`, 400 `STOCK_SHORTAGE`("{상품명} 재고가 부족합니다.")

**[링크 설명]**
- 신청 성공 → `/emp/payment/{orderId}`

> `⚠️ 구현 노트`: `OrderCreateRequest.memo`는 DTO에 정의되어 있지만 실제 저장 로직(`OrderEmpService`)에서 `OrderRequest.memo`에 매핑되지 않는 **누락 필드**입니다.

---

### 6.3 발주 결제(예치금) (직원) `(NEW)`
*소스: `PaymentEmpPage.jsx`, `PaymentController.java`*

**[화면 설명]**
- 주문 요약(주문번호/일시/배송지/주문 상품), 결제 정보(총 상품 금액/현재 예치금/최종 결제 금액/결제 후 예상 잔액)
- 예치금 부족 시 안내 문구 + 결제 버튼 비활성화

**[데이터 연동]**
- API: `POST /api/payment/deposit` — 요청(`PaymentDto.Request`): `orderId`, `amount`
  - 비즈니스: 발주 상태 `REQUESTED` 확인 → 금액 일치 검증 → 예치금 차감(`DepositHistory` PAYMENT 기록) → 발주 상태 `PAID`로 전이
  - 응답: `{orderId, paidAmount, remainingBalance, message}`
  - 예외: 404 `ORDER_NOT_FOUND`, 403 `FORBIDDEN`, 400 `INVALID_ORDER_STATUS`/`AMOUNT_MISMATCH`/`INSUFFICIENT_BALANCE`, 404 `ACCOUNT_NOT_FOUND`

**[링크 설명]**
- `결제하기` 클릭 → confirm("정말 예치금으로 결제하시겠습니까?") → 성공 시 `/emp/orders` 이동

---

### 6.4 발주 내역 목록/상세 (직원)
*소스: `OrderEmpPage.jsx`, `OrderEmpDetailPage.jsx`, `OrderEmpController.java`*

**[화면 설명]**
- 목록: 주문번호/신청일시/대표 상품명/총 금액/상태/결제 버튼(테이블, 페이징 없음, `REQUESTED` 상태는 목록에서 제외)
- 상세: 주문 정보(신청일시/진행상태/총금액/반려사유), 배송 정보(상태/운송장번호/수신처/수신자/연락처), 품목 테이블, `발주 취소`/`결제하기` 버튼
- 상태 뱃지 7종: 결제대기(승인대기)/결제완료/승인됨/부분승인/처리완료/반려됨/발주취소

**[데이터 연동]**
- 목록: `GET /api/emp/orders` — `List<OrderListResponse>`
- 상세: `GET /api/emp/orders/{orderId}` — `OrderDetailResponse`(배송 없으면 "배송 대기", 운송장 없으면 "등록된 운송장이 없습니다.") / 예외: 404 `ORDER_NOT_FOUND`, 403 `FORBIDDEN`
- 취소: `PATCH /api/emp/orders/{orderId}/cancel` — `REQUESTED`는 상태만 취소, `PAID`는 **예치금 전액 환불 후 취소**, 그 외 상태는 취소 불가(400 `INVALID_STATUS`)

**[링크 설명]**
- 목록 행 클릭 → `/emp/orders/{id}` · `결제하기` → `/emp/payment/{orderId}`
- 상세 `발주 취소` → confirm 후 취소 API, 성공 Toast(환불 여부에 따라 문구 상이)

---

### 6.5 발주 관리 목록 (관리자)
*소스: `OrderAdmPage.jsx`, `OrderAdmController.java`*

**[화면 설명]**
- 요약 카드: 총 누적 발주/금일 신규 발주/승인 완료/반려 처리
- 탭: 전체/승인 대기/부분 처리/완료/취소·반려, 버튼: `엑셀 다운로드`, `새로고침`
- 체크박스 일괄 선택 → `선택 발주 일괄 승인`, 테이블(주문번호/요청자/대표 품목/총 금액/주문일시/상태/상세보기)
- 일괄 승인 결과 모달: 승인 완료/재고부족 반려/처리 실패 건수

**[데이터 연동]**
- 목록: `GET /api/admin/orders?status=` (ADMIN, 상태 생략 시 전체) — `List<OrderAdminResponse>`
- 일괄 승인: `POST /api/admin/orders/bulk-approve` — 요청 `{orderIds: List<Long>}` (`PAID` 상태만 대상)
  - 응답(`BulkOrderResponse`): `successCount`, `autoRejectCount`(재고부족 자동반려), `failureCount`, `failures[]{orderId,reason}`
  - 비즈니스: 건별 **독립 트랜잭션(REQUIRES_NEW)** 으로 처리하여 한 건의 실패가 다른 건에 영향 없음
- 엑셀 다운로드: `GET /api/admin/orders/excel` — `application/vnd...spreadsheetml.sheet` 파일 스트림(Apache POI), 파일명 `발주내역리스트_YYYYMMDD.xlsx`

**[링크 설명]**
- `상세보기`/대표 품목 클릭 → `/admin/orders/{id}`

---

### 6.6 발주 상세 처리 (관리자)
*소스: `OrderAdmDetailPage.jsx`, `OrderAdmController.java`*

**[화면 설명]**
- 정보: 매장명/신청자/신청일시/총 금액/반려 사유
- `대기 품목 전체 승인` 버튼, 품목 테이블(상품명/수량/단가/소계/상태/승인·반려·대기 버튼)

**[데이터 연동]**
- 상세: `GET /api/admin/orders/{orderId}` — `OrderAdminDetailResponse`(items[].status는 OrderDetailStatus: WAITING/APPROVED/DELAYED/REJECTED)
- 처리: `PATCH /api/admin/orders/{orderId}/process` — 요청(`OrderProcessRequest`): `items[]{orderDetailId, status}`
  - 비즈니스: `REQUESTED`(미결제) 처리 불가(400 `NOT_PAID_ORDER`) · 목표 APPROVED 시 비관적 락으로 재고 차감 · 목표 REJECTED 시 품목별 부분 환불 · 전체 결과 집계 후 발주 상태 자동 갱신(전체승인→COMPLETED+배송자동생성, 전체반려→REJECTED, 혼합→PARTIAL)
  - 예외: 404 `ORDER_NOT_FOUND`/`ORDER_DETAIL_NOT_FOUND`/`ITEM_NOT_FOUND`/`ADMIN_NOT_FOUND`, 400 `EMPTY_ORDER_ITEMS`

**[링크 설명]**
- 개별 승인/반려/대기 버튼 → 즉시 API 호출(비동기), 성공 시 목록 재조회 없이 상태만 갱신
- `대기 품목 전체 승인` → 대기 품목이 없으면 info Toast, 있으면 일괄 승인 처리

> `⚠️ 구현 노트`: 원본 스토리보드는 발주 상태를 "발주 가능/발주 지연/발주 불가" 3종으로 명명했으나, 실제 구현은 `WAITING(대기)/APPROVED(승인)/DELAYED(지연)/REJECTED(반려)` 4종 상태(`OrderDetailStatus`)로 확장되었습니다.

---

## 7. 배송 관리 `(NEW)`

### 7.1 배송 현황 (직원)
*소스: `DeliveryEmpPage.jsx`, `DeliveryEmpController.java`*

**[화면 설명]**
- 탭: 전체 내역/배송 준비중/배송 중🚚/배송 완료✅
- 테이블: 주문번호/대표 상품명/배송지/배송 상태/운송장 번호/발송일, 행 클릭 시 발주+배송 상세 모달

**[데이터 연동]**
- API: `GET /api/emp/deliveries?status=&page=&size=10&sort=createdAt,DESC` (EMPLOYEE, ADMIN, 본인 발주 건만 필터)
- 응답: `Page<DeliveryDto.ListItem>`(deliveryId, orderId, receiverName, status, trackingNumber, createdAt, shippedAt, deliveredAt)
- 예외: 401 `UNAUTHORIZED`(인증 정보 없음)

**[링크 설명]**
- 행 클릭 → 모달에서 `GET /api/emp/orders/{orderId}` 호출해 상세 표시, `닫기`로 종료

---

### 7.2 배송 관리 (관리자)
*소스: `DeliveryAdmPage.jsx`, `DeliveryController.java`*

**[화면 설명]**
- 탭: 전체/배송 준비/배송 중/배송 완료
- 테이블: 주문번호/수령인/배송 상태/운송장 번호/등록일/발송일/완료일/액션(`🚀 발송 처리`, `배송 완료`)

**[데이터 연동]**
- 목록: `GET /api/admin/deliveries?status=&page=&size=10&sort=createdAt,DESC` (ADMIN) — `Page<DeliveryDto.ListItem>`
- 배송 시작: `PATCH /api/admin/deliveries/orders/{orderId}/start` — 요청(`StartShippingRequest`): `trackingNumber(@Size(max=100), optional)`, `shippedAt(optional)` — `READY→SHIPPING`, 미입력 시 자동발급 운송장 유지 · 예외: 400 `INVALID_DELIVERY_STATUS`("배송 준비 상태에서만...")
- 배송 완료: `PATCH /api/admin/deliveries/orders/{orderId}/complete` — 요청 `{deliveredAt(optional)}` — `SHIPPING→COMPLETED` · 예외: 400 `INVALID_DELIVERY_STATUS`("배송 중 상태에서만...")
- 상세: `GET /api/admin/deliveries/orders/{orderId}` — 예외: 404 `DELIVERY_NOT_FOUND`(발주가 COMPLETED가 아니면 배송 자체가 없음)

**[링크 설명]**
- `🚀 발송 처리`/`배송 완료` 버튼 → confirm → 비동기 처리 후 목록 즉시 갱신(새로고침 없음)

> **배송 생성 로직**: 발주 상세 처리(6.6)에서 전 품목이 `APPROVED`되어 `OrderStatus.COMPLETED`로 전이되면 `OrderApprovedEvent`(AFTER_COMMIT) → `OrderDeliveryEventListener`가 수신자명·연락처·주소가 모두 채워진 경우에만 배송(`Delivery`, 초기상태 `READY`)을 자동 생성합니다. 정보 누락 시 400 `ORDER_SHIPPING_SNAPSHOT_MISSING`.

---

## 8. 예치금 관리 `(NEW)`

### 8.1 나의 예치금 (직원)
*소스: `DepositEmpPage.jsx`, `DepositController.java`*

**[화면 설명]**
- 사용 가능한 예치금 잔액, `충전하기` 버튼, 거래 내역 테이블(거래일시/구분/상세내용/금액), 뱃지(충전/환불/결제)
- 모달: 충전 금액 입력 + 빠른 금액 버튼(+1만/+5만/+10만)

**[데이터 연동]**
- 조회: `GET /api/emp/deposit?page=&size=` — `DepositEmpDto.HistoryResponse{currentBalance, histories: Page<HistoryItem>}` (계좌 없으면 잔액 0 반환)
- 충전: `POST /api/deposit/charge` — 요청 `{amount, description}` → 응답 `{userId, currentBalance, message}` · 예외: 400 `INVALID_AMOUNT`

**[링크 설명]**
- `충전하기` → 모달 → 충전 API 호출 후 잔액/내역 즉시 갱신

> `⚠️ 구현 노트`: `POST /api/deposit/charge`는 **즉시 충전**(승인 절차 없음) API이며, 아래 8.2의 "충전 요청→관리자 승인" 플로우와는 별개의 경로입니다. 실제 `DepositEmpPage`는 즉시충전 API를 사용합니다.

---

### 8.2 예치금 충전 요청 / 승인·반려 `(NEW)`
*소스: `ChargeEmpController.java`, `ChargeAdmController.java` (전용 프론트 화면 미발견)*

**[화면 설명]**
- `[미구현]` 현재 프론트엔드 페이지 목록에 "충전 요청 내역" 및 "관리자 충전 승인/반려" 전용 화면이 존재하지 않습니다. 아래는 백엔드에 이미 구현된 API 명세입니다.

**[데이터 연동]**
- 요청: `POST /api/emp/charges` (ROLE_EMPLOYEE) — 요청 `{amount}` → `ChargeRequest`(상태 `PENDING`) 생성 · 예외: 400 `INVALID_AMOUNT`
- 내 요청 내역: `GET /api/emp/charges` — `List<ChargeDto.Response>`(id, requestUserName, storeName, amount, status, requestDate, processDate, processorName, rejectReason)
- 관리자 대기 목록: `GET /api/admin/charges/pending` (ROLE_ADMIN) — `PENDING` 상태만
- 승인: `PATCH /api/admin/charges/{chargeId}/approve` — `PENDING→APPROVED` + 실제 예치금 지급 · 예외: 400 `ALREADY_PROCESSED`
- 반려: `PATCH /api/admin/charges/{chargeId}/reject` — 요청 `{reason}` — `PENDING→REJECTED`

**[링크 설명]**
- (화면 미구현으로 해당 없음 — 향후 `/emp/charges`, `/admin/charges` 라우트·페이지 추가 필요)

---

### 8.3 예치금 관리 (관리자)
*소스: `DepositAdmPage.jsx`, `AdminDepositController.java`*

**[화면 설명]**
- 요약: 총 예치금 잔액(본사 보관금)/이번 달 총 충전액/이번 달 발주 사용액
- 필터: 거래유형 셀렉트(전체/충전만/결제만), 가맹점명 검색, `가맹점 수동 지급` 버튼
- 테이블: 발생일시/가맹점명/구분/상세내용/발생금액/거래후잔액, 뱃지(충전+/사용-/환불-/결제-)
- 모달: 대상 가맹점 선택, 지급 금액, 지급 사유

**[데이터 연동]**
- 목록: `GET /api/admin/deposits?storeId=&type=&keyword=&page=&size=` — `AdminDepositDto.ListResponse{summary{totalBalance,monthlyCharge,monthlyUsage}, histories: Page<HistoryItem>}`
- 가맹점 목록: `GET /api/admin/deposits/franchisees` — `List<FranchiseeInfo>{userId,userName,email,storeName}`
- 수동 지급: `POST /api/admin/deposits/charge` — 요청(`AdminChargeRequest`): `targetUserId`, `amount`, `description` → 응답 `{userId, currentBalance, message}`

**[링크 설명]**
- `가맹점 수동 지급` → 모달 → 지급 API 호출 → 목록 즉시 갱신

---

## 9. 직원(회원) 관리

### 9.1 직원 목록 (관리자)
*소스: `UserAdmPage.jsx`, `AdminUserController.java`*

**[화면 설명]**
- 요약: 총 직원/재직 중/휴직/잠긴 계정
- 필터: 매장 셀렉트, 상태 셀렉트(재직중/휴직/퇴사), 검색(이름/이메일/연락처)
- 테이블: 회원 정보/소속 매장/상태/계정 잠금/가입일/관리, 뱃지(재직중/휴직/퇴사/잠김/정상/Admin)

**[데이터 연동]**
- API: `GET /api/admin/users?storeId=&status=&keyword=&page=&size=&sort=` (ADMIN)
- 응답(`AdminUserDto.ListResponse`): `summary{total,active,leave,locked}`, `users: Page<UserListItem>`(id,name,email,phone,storeId,storeName,status,isLocked,isAdmin,createdAt)

**[링크 설명]**
- 행 클릭/`관리` → `/admin/users/{id}` (목록에 상세 데이터가 이미 포함되어 있어 **별도 상세 조회 API는 존재하지 않음** — state로 전달)

---

### 9.2 직원 상세 (관리자)
*소스: `UserDetailAdmPage.jsx`, `AdminUserController.java`*

**[화면 설명]**
- 요약: 재직 상태/계정 잠금, `계정 잠금 해제` 버튼
- 폼: 소속 매장, 재직 상태, `관리자(ADMIN) 권한 부여` 체크박스
- 보안 설정: `비밀번호 초기화 메일 발송`, `변경사항 저장`

**[데이터 연동]**
- 수정: `PUT /api/admin/users/{id}` — 요청(`UpdateRequest`): `storeId`, `status`(UserStatus), `isAdmin` — `RESIGNED` 처리 시 `deleted=true`, `isAdmin` 토글 시 `ROLE_ADMIN` 부여/회수 · 예외: 404 `USER_NOT_FOUND`/`STORE_NOT_FOUND`
- 잠금 해제: `PATCH /api/admin/users/{id}/unlock` — 실패횟수/잠금 초기화
- 비밀번호 초기화 강제발송: `POST /api/admin/users/{id}/reset-password`

**[링크 설명]**
- `계정 잠금 해제`/`비밀번호 초기화 메일 발송` → confirm 후 즉시 처리
- `변경사항 저장` 성공 → `/admin/users`로 복귀

---

### 9.3 내 정보 조회/수정 (직원/관리자 공통)
*소스: `ProfileEmpPage.jsx`, `UserController.java`*

**[화면 설명]**
- 조회: 이메일/이름/매장명/생년월일/핸드폰 번호/상태(재직중/휴직/퇴사 뱃지)
- 수정 폼: 이메일·상태(수정불가), 이름/매장명/핸드폰 번호(수정가능)
- `비밀번호 변경` 모달: 현재 비밀번호/새 비밀번호/새 비밀번호 확인

**[데이터 연동]**
- 조회: `GET /api/user/profile` — `UserProfileResponse{email,name,storeName,birthday,phone,status}`
- 수정: `PUT /api/user/profile` — 요청(`UserProfileUpdateRequest`): `name(@NotBlank)`, `storeName`, `phone(@NotBlank)`, `password`(선택) · 예외: 404 `STORE_NOT_FOUND`
- 비밀번호 변경: `PATCH /api/user/profile/password` — 요청 `{password(현재), newPassword}` · 실패: 400 "현재 비밀번호가 일치하지 않습니다."

**[링크 설명]**
- `정보 수정하기` → 인라인 폼 전환 → `수정 완료`
- `비밀번호 변경` → 모달 → `변경하기`

---

## 10. 문의사항 관리

### 10.1 문의사항 목록 (직원/관리자)
*소스: `InquiryEmpPage.jsx`, `InquiryAdmPage.jsx`, `InquiryController.java`*

**[화면 설명]**
- 직원: `+ 문의 작성하기` 버튼 + 테이블(번호/상태/제목/등록일), 뱃지(답변완료/답변대기)
- 관리자: 테이블(번호/상태/제목/작성자(가맹점)/등록일) — 작성 버튼 없음

**[데이터 연동]**
- API: `GET /api/inquiry?page=&size=` — `Page<InquiryListResponse>`(inquiryId,title,authorName,isRead,commentCount,createdAt)
- 비즈니스: **ADMIN은 전체 문의**, **EMPLOYEE는 본인 작성 문의만** 조회(매장 단위 공유 아님)

**[링크 설명]**
- 직원 `+ 문의 작성하기` → `/emp/inquiries/new`
- 행 클릭 → `/emp/inquiries/{id}` 또는 `/admin/inquiries/{id}`

> `⚠️ 구현 노트`: Swagger 문서 상 설명(소속 매장 전체 문의 공유)과 달리, 실제 Repository 쿼리는 **작성자 본인 문의만** 반환합니다.

---

### 10.2 문의사항 작성 (직원)
*소스: `InquiryCreatePage.jsx`, `InquiryController.java`*

**[화면 설명]**
- 제목*, 내용*, 첨부파일(선택, 안내: "10MB 이하 이미지/PDF"), `취소`/`문의 등록하기`

**[데이터 연동]**
- API: `POST /api/inquiry` (ROLE_EMPLOYEE, `multipart/form-data`) — 요청(`InquiryCreateRequest`): `title(@NotBlank,@Size(max=200))`, `content(@NotBlank)`, `file(선택)`
- 비즈니스: 첨부파일은 `./uploads/inquiries/`에 UUID 파일명으로 저장 · 예외: 500 `FILE_UPLOAD_ERROR`

**[링크 설명]**
- `문의 등록하기` 성공 → `/emp/inquiries`

---

### 10.3 문의사항 상세 조회 및 댓글/답글 (직원/관리자)
*소스: `InquiryDetailEmpPage.jsx`, `InquiryDetailAdmPage.jsx`, `InquiryController.java`, `InquiryCommentController.java`*

**[화면 설명]**
- 상세: 제목/내용/첨부파일 다운로드, 댓글 목록(작성자/시각), 댓글 입력창 + `등록`
- 댓글별 `답글`/`수정`/`삭제` 액션(2단계까지만 허용 — 답글에는 답글 불가)
- 관리자 화면은 `답변 및 피드백` 라벨로 표기, 미확인 문의는 상세 진입 시 자동으로 읽음 처리

**[데이터 연동]**
- 상세: `GET /api/inquiry/{inquiryId}` — `InquiryDetailResponse{...,comments:[{id,content,authorName,authorEmail,authorId,createdAt,parentId}]}` (ADMIN이 열람 시 자동 `markAsRead()`) · 예외: 403 `FORBIDDEN`(본인 아닌 직원의 열람 시도)
- 댓글 등록: `POST /api/inquiry/{inquiryId}/comments` — 요청 `{content(@NotBlank), parentId(선택)}` · 예외: 400 `INVALID_COMMENT_DEPTH`(답글의 답글 금지)
- 댓글 수정: `PUT /api/inquiry/{inquiryId}/comments/{commentId}` — 작성자 본인만(403 `FORBIDDEN`)
- 댓글 삭제: `DELETE /api/inquiry/{inquiryId}/comments/{commentId}` — 작성자 본인만
- 문의글 삭제: `DELETE /api/inquiry/{inquiryId}` — **작성자 본인만**(관리자도 타인 글 삭제 불가)

**[링크 설명]**
- `목록으로` → `/emp/inquiries` 또는 `/admin/inquiries`
- `다운로드` → 첨부파일 스트리밍 다운로드(`GET` via apiClient)
- `글 삭제` 성공(직원, 본인 글) → 목록으로 이동

---

## 11. 연차(휴가) 관리 `(NEW)`

### 11.1 연차 신청 (직원)
*소스: `VacationRegisterEmpPage.jsx`, `AnnualLeaveEmpController.java`*

**[화면 설명]**
- 시작일/종료일(date input), 연차 종류 Select(연차/반차/병가), 사유 Textarea
- 클라이언트 검증: 시작일 > 종료일이면 `window.alert()`로 1차 방어
- 제출 성공 시 "연차 신청이 완료되었습니다" 안내 후 목록 이동

**[데이터 연동]**
- API: `POST /api/emp/vacation` (EMPLOYEE, ADMIN)
- 요청(`CreateRequest`): `startDate`, `endDate`, `type`(ANNUAL/HALF_DAY/SICK), `reason`
- 비즈니스: 시작일>종료일 또는 기존 신청과 기간 겹침(반려건 제외) 시 `InoutException` 발생, 신규 신청은 항상 `PENDING`
- 예외: 400 (기간 오류), 400 (기간 중복)

**[링크 설명]**
- 제출 성공 → Toast 성공 메시지 후 `/emp/vacation`(목록) 이동

---

### 11.2 연차 신청 목록/상세 (직원)
*소스: `VacationEmpPage.jsx`, `VacationEmpDetailPage.jsx`, `AnnualLeaveEmpController.java`*

**[화면 설명]**
- 목록: 신청일자/연차기간/종류/상태(대기/승인/반려/보류) 뱃지, 페이지네이션
- 상세: 반려 상태인 경우 관리자가 입력한 반려 사유를 화면에 명확히 노출

**[데이터 연동]**
- 목록: `GET /api/emp/vacation?page=&size=` — `Page<ListItem>`
- 상세: `GET /api/emp/vacation/{leaveId}` — `DetailResponse`(rejectReason 포함) · 예외: 403(타인 신청 조회 시)

**[링크 설명]**
- 목록 항목 클릭 → `/emp/vacation/{leaveId}`
- `연차 신청` 버튼 → `/emp/vacation/new`

---

### 11.3 연차 심사 및 처리 (관리자)
*소스: `VacationAdmPage.jsx`, `AnnualLeaveAdmController.java`*

**[화면 설명]**
- 상태 탭 필터(대기/승인/반려/보류/전체), 목록 테이블(신청자/기간/종류/상태)
- 각 행 우측 `승인`/`반려`/`보류` 버튼
- `반려` 클릭 시 반려 사유 입력 모달(RejectModal) 활성화, 사유 입력 후 최종 처리

**[데이터 연동]**
- 목록: `GET /api/admin/vacation?status=&page=&size=` (ROLE_ADMIN) — `Page<ListItem>`
- 처리: `PATCH /api/admin/vacation/{leaveId}` — 요청(`ProcessRequest`): `status`(APPROVED/REJECTED/HOLD), `rejectReason`(REJECTED일 때 필수)
- 비즈니스: `REJECTED` 처리 시 `rejectReason` 없으면 `InoutException` 발생. 이미 처리된(PENDING/HOLD가 아닌) 건은 재처리 불가

**[링크 설명]**
- `승인`/`보류` → 즉시 비동기(Axios) 호출 → 성공 시 페이지 새로고침 없이 목록 상태 즉시 갱신
- `반려` → 모달 오픈 → 사유 입력 후 확인 → 처리 API 호출 → 목록 갱신 + 모달 닫힘

---

## 12. AI 자동발주 `(NEW, 백엔드 전용 — 전용 화면 없음)`

### 12.1 AI 자동발주 스케줄러 / 수동 트리거
*소스: `AiAutoOrderScheduler.java`, `AiAutoOrderService.java`, `AiInsightController.java`*

**[화면 설명]**
- `[미구현]` 현재 프론트엔드에는 전용 UI가 없습니다(대시보드의 "AI 운영 인사이트"와는 별개 기능).

**[데이터 연동]**
- 자동 실행: `@Scheduled(cron="0 0 0 * * ?")` — **매일 자정** `AiAutoOrderService.createAutoOrderDraft()` 호출
- 수동 실행: `POST /api/admin/ai/auto-order` (ROLE_ADMIN) — 응답 `{savedCount(0 또는 1), message}`
- 비즈니스: 저재고/품절 품목을 Gemini에 전달해 추천 발주 수량을 JSON으로 응답받아, **통합 발주서 1건**(`OrderRequest`, status=`REQUESTED`, requestUser=DB상 첫 ADMIN)으로 자동 생성
- 예외: 503 `GEMINI_NOT_CONFIGURED`, 500 `ADMIN_NOT_FOUND`/`AI_INVALID_RESPONSE`/`AI_PARSE_ERROR`/`AI_DESERIALIZE_ERROR`, 503 `AI_API_ERROR` — 스케줄러 실행 시 예외는 로그만 남기고 외부로 전파하지 않음

**[링크 설명]**
- 생성된 초안은 일반 발주 목록(`/admin/orders`)의 "승인 대기" 건으로 노출되어 6.5/6.6 화면에서 동일하게 처리

---

## 13. 원본 스토리보드 대비 변경/미구현 사항 총정리

### 13.1 신규 추가 기능 (코드에는 있으나 원본 스토리보드에 없던 것) — `(NEW)`

| 섹션 | 기능 | 비고 |
|---|---|---|
| 3.2 | 소셜 로그인(OAuth2: Google/Kakao/Naver) | 백엔드 완전 구현, 프론트 일부(Kakao) 준비중 |
| 4.3 | AI 운영 인사이트(Gemini) | 관리자 대시보드 통합 |
| 6.3 | 예치금 결제(발주 결제 방식 자체가 신규) | 원본은 결제 수단 불명확, 현재는 예치금 단일 결제 |
| 6.5 | 발주 일괄 승인 + 엑셀 다운로드 | 원본에 없던 관리자 생산성 기능 |
| 7장 전체 | 배송 관리(직원/관리자) | 원본 스토리보드에 배송 개념 없음 |
| 8장 전체 | 예치금 관리(직원 조회/충전, 충전요청·승인, 관리자 조회/수동지급) | 원본에 결제/예치금 개념 없음 |
| 11장 전체 | 연차(휴가) 관리(직원 신청/조회, 관리자 심사) | 금번 세션에서 신규 구현 |
| 12장 전체 | AI 자동발주(스케줄러) | 원본에 없음, 전용 화면도 아직 없음 |

### 13.2 원본에는 있으나 현재 미구현 — `[미구현]`

| 원본 섹션 | 기능 | 현재 상태 |
|---|---|---|
| 2.1 | 공통 Footer(공지사항/이용약관/개인정보취급방침) | 대응 화면 없음 |
| 3.4 | 회원가입 이용약관 동의 체크박스 | `RegisterPage`에 없음 |
| 5.8 | 재고 입고 처리 전용 화면 | API(`POST /api/admin/stocks/receive`)는 존재하나 화면 없음, 재고 실사 화면으로 대체 운용 |
| 5.9 | 전체 입고 이력 보기(전 상품 통합) | 상품별 이력만 제공, 전체 통합 조회 API/화면 없음 |
| 5.6 | 상품 사진 개별 수정/삭제 API | 현재는 상품 정보 수정 시 `imageUrl` 통째 교체만 가능 |
| 8.2 | 충전 요청→승인 플로우 전용 화면 | 백엔드 API는 존재, 프론트 화면 없음(직접 즉시충전 API만 연동) |

### 13.3 원본과 구현이 달라진 정책 — `⚠️ 구현 노트`

- 발주 상태: "발주가능/지연/불가" 3종 → `WAITING/APPROVED/DELAYED/REJECTED` 4종으로 확장, 발주 헤더 상태도 `REQUESTED→PAID→(PARTIAL|COMPLETED|REJECTED)→CANCELLED` 흐름으로 재설계(예치금 결제 단계 추가)
- 문의사항: 매장 단위 공유가 아닌 **작성자 개인 단위** 조회/삭제 권한으로 구현
- 아이디 찾기: 결과를 별도 페이지가 아닌 동일 화면 내 인라인으로 노출, 마스킹 미적용

---

*(끝 — 총 13개 대분류, 45개 상세 화면/기능 슬라이드로 구성. VBA 매크로 대응 슬라이드 번호는 별도 안내 참고)*
