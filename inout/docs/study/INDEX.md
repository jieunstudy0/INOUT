# INOUT 프로젝트 구조도 & 학습 INDEX

> B2B 프랜차이즈 발주·재고 관리 시스템 **INOUT**의 코드를 패키지 단위로 다시 뜯어보며 정리한 기록입니다.
> 각 글은 "기능 흐름" 단위로 쓰였고, 글마다 그 흐름에 참여하는 클래스(Controller · Service · DTO · Entity · Repository)를 함께 정리했습니다.

- **Backend**: Java 17 · Spring Boot 3.5 · Spring Security · JWT · OAuth2(Google) · JPA/Hibernate · MySQL 8 · Redis
- **Frontend**: React 19 · Vite · Tailwind CSS
- **AI**: Gemini 2.5 Flash
- **Infra**: Docker · Nginx · AWS EC2

---

## 0. 프로젝트 구조도

### 0-1. 사용자 역할

| 역할 | 하는 일 |
|------|---------|
| **EMPLOYEE (매장 직원)** | 장바구니 → 발주 기안, 재고 사용 처리, 연차 신청, 문의 작성 |
| **OWNER (가맹점주)** | 직원 기안 수정·결제 승인 / 반려, 직접 발주, 예치금 충전 신청, 직원·연차 관리 |
| **ADMIN (본사)** | 발주 최종 승인(개별·항목별·일괄), 재고 입고·실사, 배송 처리, 충전 승인, 회원 관리 |

### 0-2. 핵심 비즈니스 흐름 (발주 → 결제 → 재고 → 배송)

```
[직원] 장바구니 담기
   │
   ▼
[직원] 발주 기안 ─────────────────────────── OrderStatus.REQUESTED (미결제)
   │
   ▼
[점주] 기안 수정 후 결제 승인 ── 예치금 차감 ── OrderStatus.ORDERED  (payment)
   │        (또는 반려 → REJECTED)
   │
   ▼
[본사] 최종 승인 (단건 / 항목별 / 일괄)
   │    ├─ 품목별 재고 차감 (비관적 락) ─────────────────────────── (stock)
   │    ├─ 반려·재고부족 품목은 예치금 환불 ─────────────────────── (payment)
   │    └─ OrderStatus.APPROVED
   │
   ├──▶ 배송 생성 (멱등) → 운송장 발급 → 배송 중 → 배송 완료 ──── (delivery)
   └──▶ 상태 변경 이벤트 → 커밋 이후 비동기 메일 발송 ─────────── (common.mail)
```

### 0-3. 패키지 지도

```
com.jstudy.inout
├── order          발주 (핵심 도메인)
├── stock          재고·상품
├── payment        예치금·충전·환불
├── delivery       배송
├── common
│   ├── auth       회원·인증·권한
│   ├── oauth2     Google 소셜 로그인
│   ├── jwt        토큰 발급·검증
│   ├── config     보안·캐시·비동기·로깅·데이터 마이그레이션 설정
│   ├── mail       메일 템플릿·발송
│   ├── massdata   대용량 테스트 데이터 생성
│   └── dto · exception · entity · util   공통 응답·예외·기반 엔티티
├── dashboard      역할별 대시보드 집계
├── inquiry        문의 게시판
├── leave          연차
└── ai             Gemini 연동 (인사이트·자동발주·CS 분류)
```

---

## 1. 핵심 설계 결정 — 먼저 읽으면 좋은 글

> 여러 패키지에 걸쳐 있는 설계 판단을 모은 글입니다. 면접에서 가장 많이 받는 질문도 여기에 몰려 있습니다.

- [발주부터 배송까지 — 전체 흐름 한 장으로 보기](링크)
- [동시성 제어: 비관적 락(PESSIMISTIC_WRITE)과 낙관적 락(@Version)을 같이 쓴 이유](링크)
- [트랜잭션 전파: REQUIRES_NEW를 별도 클래스로 분리한 이유 (프록시와 self-invocation)](링크)
- [이벤트 기반 후처리: @TransactionalEventListener(AFTER_COMMIT) + @Async](링크)
- [Redis 캐시와 장애 대응: Redis가 죽어도 서비스가 멈추지 않게](링크)
- [공통 응답 규약과 전역 예외 처리: ResponseResult · InoutException · GlobalExceptionHandler](링크)

---

## 2. 핵심 비즈니스 흐름

### order — 발주
직원 기안 → 점주 결제 승인 → 본사 최종 승인으로 이어지는 3단계 결재형 발주 도메인.

- [발주 도메인 모델과 상태 전이 (OrderRequest · OrderDetail · OrderStatus)](링크)
- [장바구니 흐름 (담기 · 수정 · 삭제 · 재주문)](링크)
- [직원 발주 기안 흐름 (미리보기 · 신청 · 조회 · 취소)](링크)
- [점주 결재 흐름 (기안 수정 후 결제 승인 · 반려)](링크)
- [점주 직접 발주 흐름 (즉시 결제)](링크)
- [본사 승인 흐름 ① 단건 승인 · 반려와 항목별 처리 (부분 승인 · 지연 · 환불)](링크)
- [본사 승인 흐름 ② 일괄 승인 (건별 독립 트랜잭션)](링크)
- [이벤트 기반 알림 (OrderStateChangedEvent → 메일)](링크)
- [운영 기능 (엑셀 다운로드 · 레거시 상태 마이그레이션)](링크)

### stock — 재고
상품 마스터와 재고 수량, 그리고 입고·사용 이력을 관리하는 도메인.

- [재고 도메인 모델 (Item · @Version · 이력 테이블 상속 구조)](링크)
- [상품 관리 흐름 (등록 · 수정 · 논리 삭제 · 이미지 업로드)](링크)
- [입고 흐름 (본사 입고 처리)](링크)
- [재고 사용 흐름 (직원 사용 처리 · 재고 부족 예외)](링크)
- [재고 실사 흐름 (재고 조정)](링크)
- [재고 조회 · 이력 · 저재고 알림 (+ 캐시 적용)](링크)

### payment — 결제 · 예치금
매장 단위 예치금 계좌로 발주 대금을 결제·환불하고, 충전을 승인받는 도메인.

- [예치금 도메인 모델 (DepositAccount · DepositHistory · TransactionType)](링크)
- [발주 결제와 환불 (예치금 차감 · 환불과 계좌 락)](링크)
- [충전 흐름 (점주 충전 신청 → 본사 승인 · 반려)](링크)
- [직원 1일 예치금 사용 한도 (한도 설정 · 자정 초기화 스케줄러)](링크)
- [예치금 조회 (직원 · 점주 · 본사 전체 내역)](링크)
- [레거시 · 비활성 API 정리 회고](링크)

### delivery — 배송
본사 승인 이후 배송을 생성하고 상태(READY → SHIPPING → COMPLETED)를 관리하는 도메인.

- [배송 도메인 모델과 상태 전이](링크)
- [배송 생성 흐름 (발주 승인 연동 · 멱등 생성)](링크)
- [본사 배송 처리 흐름 (운송장 발급 → 배송 시작 → 배송 완료)](링크)
- [역할별 배송 조회와 배송 추적](링크)
- [배송 자동 완료 스케줄러](링크)

---

## 3. 신뢰성 · 보안

### common.auth — 회원 · 인증 · 권한
- [회원 도메인 모델 (User · Role · UserRole · Store · UserStatus)](링크)
- [회원가입 흐름 (가입 · 이메일 중복 확인)](링크)
- [로그인 · 토큰 재발급 · 로그아웃 흐름](링크)
- [아이디 찾기 · 비밀번호 재설정 흐름 (메일 링크)](링크)
- [내 프로필 조회 · 수정 · 비밀번호 변경](링크)
- [점주의 직원 관리 (계정 생성 · 상태 변경 · 잠금 해제 · 예치금 한도)](링크)
- [본사의 회원 관리 (목록 · 권한 수정 · 잠금 해제 · 비밀번호 초기화)](링크)
- [인증 주체 CustomUserDetails와 @AuthenticationPrincipal](링크)

### common.oauth2 — 소셜 로그인
- [Google OAuth2 로그인 흐름 (CustomOAuth2UserService → SuccessHandler)](링크)
- [소셜 가입자 온보딩 (추가 정보 입력 · 매장 연결)](링크)

### common.jwt — 토큰
- [JWT 발급 · 검증 구조 (JwtTokenProvider)](링크)
- [요청마다 인증이 처리되는 과정 (JwtAuthenticationFilter → SecurityContext)](링크)
- [Access Token / Refresh Token 전략](링크)

### common.config — 애플리케이션 설정
- [Spring Security 설정 (SecurityConfig · 401/403 핸들러)](링크)
- [캐시 · Redis 설정 (CacheConfig · ResilientCacheManager · LoggingCacheErrorHandler)](링크)
- [비동기 · JPA Auditing · Web · Swagger 설정](링크)
- [요청 추적 로깅 (MdcLoggingFilter · logback)](링크)
- [데이터 마이그레이션 러너 (ApplicationRunner로 운영 데이터 정리)](링크)
- [더미 데이터 초기화 · 스케줄러](링크)

---

## 4. 운영 · 지원

### dashboard — 대시보드
- [역할별 대시보드 집계 (본사 · 점주 · 직원)](링크)
- [대시보드 캐싱과 캐시 무효화](링크)

### inquiry — 문의
- [문의 작성 · 상세 · 삭제 (파일 첨부 · 다운로드)](링크)
- [문의 대상별 목록 (직원→점주 · 직원→본사 · 점주→본사)](링크)
- [댓글 · 답댓글 흐름](링크)

### leave — 연차
- [연차 신청 흐름 (직원 신청 · 잔여 연차)](링크)
- [연차 결재 흐름 (점주 승인 · 반려 · 보류 / 본사 모니터링)](링크)

### ai — Gemini 연동
- [AI 경영 인사이트 리포트 (@Async + CompletableFuture)](링크)
- [AI 자동 발주 초안 생성 (자정 스케줄러)](링크)
- [AI 문의 자동 분류 · 답변 초안 (10분 스케줄러 · REQUIRES_NEW)](링크)
- [AI 스마트 발주 추천 (재고 연동)](링크)

### common.mail — 메일
- [메일 템플릿 관리와 캐싱 (MailTemplate · MailTemplateCacheService)](링크)
- [발주 상태 · 비밀번호 재설정 메일 발송 (MailComponent)](링크)

---

## 5. 공통 기반

### common — 응답 · 예외 · 기반 엔티티
- [공통 응답 포맷 (ResponseResult · ResponseMessage · ResponseMessageHeader)](링크)
- [커스텀 예외 계층 (InoutException과 하위 예외)](링크)
- [BaseTimeEntity와 JPA Auditing](링크)
- [파일 저장 유틸 (FileStorageUtil)](링크)

### common.massdata — 대용량 데이터
- [대용량 테스트 데이터 생성기 (MassDataGeneratorService)](링크)

---

## 6. 회고 · 개선 기록
- [코드 재학습 중 발견한 개선 포인트 모음](링크)
- [배포 기록 (Docker · Nginx · EC2)](링크)
