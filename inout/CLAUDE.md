# INOUT 프로젝트 — Claude Code 가이드

## 프로젝트 개요
B2B 발주·재고 관리 시스템. Spring Boot 백엔드 + React(Vite) 프론트엔드 구성.

## 기술 스택
- **Backend**: Java 17, Spring Boot 3.5.7, JPA/Hibernate, MySQL 8, JWT, Spring Security
- **Frontend**: React 19 + Vite + Tailwind CSS (frontend/ 폴더)
- **Cache**: Redis (Spring Cache Abstraction)
- **AI**: Gemini API 연동 — 자동 발주·문의 답변·실사 영수증 읽기는 `gemini.model` 설정(현재 `gemini-2.5-flash`. `gemini-2.0-flash`는 서비스 종료로 404), 인사이트 리포트는 `gemini-2.5-flash`를 코드에 직접 적어 사용 (설정 하나로 통일 예정)
- **Test**: JUnit5, Mockito, MockMvc, Spring Security Test — 개수 대신 `./gradlew test` 전체 통과로 확인
- **Build**: Gradle 8

## 폴더 구조
```
INOUT/
├── README.md
└── inout/
    ├── CLAUDE.md                  ← 현재 파일
    ├── build.gradle
    ├── DEPLOY.md · DEPLOY_LOG.md  ← EC2 배포 가이드와 이력
    ├── docs/
    │   ├── ARCHITECTURE.md
    │   ├── TROUBLESHOOTING.md
    │   ├── study/                ← 코드 재학습 문서 (INDEX, CLASS_MAP, PROJECT_GUIDE 등)
    │   └── storyboard/
    ├── frontend/                  ← React + Vite
    │   ├── src/
    │   │   ├── api/              ← API 모듈
    │   │   ├── components/       ← 공통 컴포넌트
    │   │   ├── pages/            ← 화면
    │   │   └── utils/            ← resultCodes.js 등
    │   └── package.json
    └── src/main/java/com/jstudy/inout/
        ├── common/               ← 인증, JWT, OAuth2, 예외처리, 공통 응답, 설정, 메일
        ├── order/                ← 발주 (핵심 도메인)
        ├── payment/              ← 결제·예치금
        ├── stock/                ← 재고 (본사 상품·재고 + 매장 재고·수령 확인·실사)
        ├── delivery/             ← 배송
        ├── inquiry/              ← 문의게시판
        ├── leave/                ← 연차
        ├── dashboard/            ← 대시보드
        ├── notification/         ← 화면 안 알림 (커밋 이후 @Async 저장)
        ├── request/              ← 현장 수정 요청 (직원·점주 → 점주·본사)
        └── ai/                   ← Gemini AI 연동 (실사 영수증 사진 읽기 포함)
```

## 재고는 두 종류다
- **본사 재고** `Item.currentStock`: 발주 본사 승인 때 줄고, 본사 입고·조정으로 늘어난다. 점주·본사가 발주 흐름으로 다룬다.
- **매장 재고** `StoreStock`(매장 × 상품): 매장이 배송을 [수령 확인]하면 받은 만큼 늘고, 직원 사용(차감)·점주 조정·점주가 승인한 실사로 바뀐다. 모든 증감은 `StoreStockHistory`에 남는다.
- 직원은 매장 재고를 **사용(차감)만** 할 수 있다. 조정·처음 등록·안전재고·위치는 점주만. 매장 실사 승인도 점주.

## 핵심 설계 원칙
1. **동시성**: 비관적 락(PESSIMISTIC_WRITE) + 낙관적 락(@Version) 병행
2. **트랜잭션**: REQUIRES_NEW는 반드시 별도 클래스로 분리 (Spring 프록시 우회 방지)
3. **이벤트**: 메일 발송은 @TransactionalEventListener(AFTER_COMMIT) + @Async로 분리
4. **캐시**: Redis 장애 시 ResilientCacheManager와 LoggingCacheErrorHandler가 두 겹으로 캐시 오류를 삼키고 DB에서 직접 조회한다 (CacheConfig)
5. **응답 규약**: 모든 API는 ResponseResult → ResponseMessage 래핑 사용
   - 실패 문구는 백엔드(header.message)가, 성공 문구는 프론트엔드가 정한다. 성공 응답의 message는 화면에 표시되지 않는다.
   - 검증 오류는 body에 `[{ field, message }]`로 담기며, 프론트는 `toFieldErrors(err.errors)`로 입력칸별 오류를 표시한다.
   - 프론트의 실패 분기는 문구가 아니라 `err.resultCode`(= ErrorCode 이름)로 한다 (`utils/resultCodes.js`, 인터셉터 `RESULT_CODE_ACTIONS`).

## 자주 쓰는 명령어

### 백엔드 빌드 및 실행
```bash
cd inout
./gradlew bootRun
./gradlew test
./gradlew test --tests "com.jstudy.inout.order.service.OrderApprovalPartialStockTest"   # 특정 테스트만
./gradlew build
```

### 프론트엔드 실행
```bash
cd inout/frontend
npm install
npm run dev
```

### Git 작업
작업은 브랜치에서 하고 PR로 main에 합친다. `git add .` 대신 바꾼 파일을 골라서 추가한다 (작업 중인 다른 파일이 섞이지 않게).
```bash
git switch -c fix/설명
git status
git add <바꾼 파일>
git commit -m "fix: 설명"
git push -u origin fix/설명      # 이후 PR 생성
```

## 코드 작성 규칙

### 커밋 메시지 형식
```
feat: 새 기능 추가
fix: 버그 수정
refactor: 리팩토링 (기능 변경 없음)
test: 테스트 추가/수정
chore: 빌드, 설정 변경
docs: 문서 수정
```

### Java 코드 규칙
- 엔티티는 @Setter 금지, 도메인 메서드로 상태 변경
- 트랜잭션은 빠짐없이 명시한다. 조회 메서드는 `@Transactional(readOnly = true)`, 쓰기 메서드는 `@Transactional`. 클래스에 `readOnly = true`를 붙인 서비스에서는 쓰기 메서드에만 `@Transactional`을 붙인다
- 예외는 반드시 `new InoutException(ErrorCode.X)` 또는 `new InoutException(ErrorCode.X, "상황별 문구")` 사용.
  새 실패 상황은 `common/exception/ErrorCode`에 상수를 추가한다(상태·resultCode·기본 문구). 문구 규칙은 `ErrorCodeTest`가 검사한다
- REQUIRES_NEW가 필요하면 반드시 별도 클래스로 분리

### 정합성·보안 규칙 (괄호는 이 규칙이 생긴 계기)
- 점주·직원 API는 URL 권한과 별도로 서비스에서 "요청 데이터의 매장 = 로그인 사용자의 매장"을 확인한다 (`CROSS_STORE_FORBIDDEN`)
  (URL 규칙만으로는 같은 점주 권한을 가진 다른 매장의 요청을 막지 못함 — `AuthServiceOwnerScopeTest`, `AnnualLeaveServiceTest`)
- 돈·재고를 바꾸는 코드는 모든 항목을 먼저 검사하고, 전부 통과했을 때만 변경한다
  (일부 품목만 차감된 채 전액 환불되던 버그 — `docs/TROUBLESHOOTING.md` 2.1, 회귀 테스트 `OrderApprovalPartialStockTest`)
- 락을 잡은 직후에는 `refresh`로 최신 값을 다시 읽는다
  (비관적 락을 잡았는데도 1차 캐시의 예전 값 때문에 낙관적 락 충돌이 나던 문제 — `docs/TROUBLESHOOTING.md` 2.2)
- 돈·재고를 바꾸는 코드를 고치면 실패 경우의 회귀 테스트를 함께 추가한다
- `@Transactional` 메서드 안에서 예외를 잡으면 롤백되지 않는다. 일부러 잡는 경우에만 쓰고 이유를 주석으로 남긴다
  (`OrderApprovalTxService`는 재고 부족 예외를 일부러 잡아 환불·반려를 커밋한다)
- 비밀번호·토큰·사용자 입력값을 로그나 응답에 그대로 남기지 않는다. 검증 실패 로그는 필드 이름과 규칙만 남긴다
  (검증 실패 로그에 비밀번호가 평문으로 남던 문제 — 커밋 `02f9104`, `GlobalExceptionHandlerValidationLogTest`)
- 메서드 보안(`@PreAuthorize`) 거부도 URL 규칙과 같은 403으로 응답한다
  (500으로 나가던 문제 — 커밋 `0175049`, `SecurityPathAclSmokeTest`)

### 바꾸면 조용히 깨지는 것 (컴파일 에러가 나지 않으니 특히 주의)
- ErrorCode 상수 이름 = API의 `resultCode`다. 이름을 바꾸면 프론트 `utils/resultCodes.js`와 `api/apiClient.js`의 `RESULT_CODE_ACTIONS` 화면 동작이 사라진다. 이름을 바꿔야 하면 프론트도 같은 PR에서 고친다
- 요청 DTO의 필드 이름 = 검증 오류의 `field` 값이다. 이름을 바꾸면 프론트 `toFieldErrors`의 입력칸별 오류 표시가 사라진다
- 응답 형식은 `ResponseMessage` 하나다. 새 응답 클래스를 만들지 않는다 (별도 형식이던 `ErrorResponse`는 커밋 `5508a9c`에서 삭제)
- `REQUIRES_NEW` 메서드를 같은 클래스 안에서 `this`로 부르면 새 트랜잭션이 열리지 않는다 (프록시를 거치지 않음)

### 하지 않는 것
- `application-secret.properties`, `.env`의 값을 출력하거나 커밋하지 않는다
- `Exception`을 통째로 잡는 것은 한 건이 실패해도 나머지를 계속해야 하는 경계(일괄 승인의 건별 처리, 커밋 이후 이벤트, 스케줄러, 외부 API 응답 처리)에서만 쓰고 반드시 로그를 남긴다. 그 밖에서는 `InoutException`으로 바꾸거나 그대로 던진다
- 메일 발송을 트랜잭션 안에서 동기로 하지 않는다 (커밋 이후 이벤트 + `@Async`)
- 엔티티에 `@Setter`, `@Data`를 붙이지 않는다

### 주의사항
- application-secret.properties는 git에 올리지 않음 (DB 비밀번호, API 키 포함)
- node_modules는 git 추적 제외 (frontend/.gitignore에 설정됨)
- Redis 없이도 실행 가능 (ResilientCacheManager·LoggingCacheErrorHandler가 캐시 오류를 삼키고 DB에서 조회)

## 작업 순서
1. 고칠 코드와 관련 테스트를 먼저 읽고, 작업 전에 `./gradlew test`로 현재 상태를 확인한다
2. 고친 뒤 관련 테스트와 전체 테스트를 다시 돌린다
3. 버그를 고쳤으면 그 버그를 다시 잡는 테스트를 추가하고, 원인과 해결을 `docs/TROUBLESHOOTING.md`에 남긴다
4. 쓰이지 않게 된 코드(클래스·메서드·DTO)는 같은 작업에서 지운다

## AI와 함께 작업하는 방식
- 설계 결정(락 방식, 트랜잭션 경계, 응답 규약, 새 의존성)은 사람이 정하고, 이유를 `docs/` 또는 PR 설명에 남긴다
- 돈·재고·권한을 바꾸는 코드는 AI가 작성해도 사람이 실패 시나리오 테스트를 먼저 확인한 뒤 합친다
- AI가 "완료"라고 보고할 때는 실행한 테스트 이름과 결과를 함께 남긴다. 실행하지 않은 것은 "확인 안 함"이라고 쓴다
- 테스트가 실제 버그를 잡는지 의심되면, 코드를 일부러 예전 방식으로 되돌려 테스트가 실패하는지 확인한다(뮤테이션 검증). 확인 후 코드는 원래대로 되돌린다
- 재학습 중 발견한 개선 포인트는 `docs/study/CLASS_MAP.md`에 날짜와 함께 기록한다

## 알려진 한계 (규칙과 실제 코드가 아직 다른 곳)
- 여러 품목의 락을 주문 상세 순서대로 잡는다. 서로 다른 순서로 락을 잡는 주문 사이에 데드락 가능성이 있어, 새 코드부터는 itemId 오름차순으로 락을 잡고 기존 코드는 개선 예정이다
- 동시성 테스트를 H2에서 돌린다. MySQL InnoDB와 락 동작이 달라 Testcontainers(MySQL)로 옮길 예정이다
- AI 모델 설정이 두 군데로 나뉘어 있다 (`gemini.model` 설정 / `AiInsightService`는 코드에 직접 적음). 하나로 통일할 예정이다
- `common/mail/dto/MailTemplate`은 엔티티인데 `@Data`를 쓰고 있고 패키지도 `dto`에 있다. `@Setter`·`@Data` 금지 규칙의 예외로 남아 있어 정리할 예정이다
- 매장 재고 행은 처음 수령·등록할 때 만든다. 같은 매장·상품 행을 두 요청이 동시에 처음 만들면 유니크 제약 위반으로 한쪽이 실패한다 (드묾, 재시도하면 됨)
- 직원 대시보드의 저재고 수치와 장바구니 AI 발주 추천은 아직 본사 재고 기준이다. 매장 재고 기준으로 바꿀 예정이다
- PWA 푸시 알림은 아직 없다 (서버 Web Push 라이브러리·VAPID 키 결정 필요). 지금 알림은 화면 안 목록 + 1분마다 안 읽은 개수 확인
- `AiInsightService.generateInsightReport`는 읽기 트랜잭션 안에서 Gemini를 호출한다. 조회와 외부 호출을 나눠 DB 연결을 오래 잡지 않게 할 예정이다

## 배포 체크리스트
- [ ] spring.jpa.properties.hibernate.show_sql=false 확인
- [ ] application-secret.properties git 미포함 확인
- [ ] ./gradlew test 전체 통과 확인
- [ ] frontend npm run build 성공 확인
- [ ] node_modules git 미추적 확인
