# INOUT 프로젝트 — Claude Code 가이드

## 프로젝트 개요
B2B 발주·재고 관리 시스템. Spring Boot 백엔드 + React(Vite) 프론트엔드 구성.

## 기술 스택
- **Backend**: Java 17, Spring Boot 3.5.7, JPA/Hibernate, MySQL 8, JWT, Spring Security
- **Frontend**: React 19 + Vite + Tailwind CSS (frontend/ 폴더)
- **Cache**: Redis (Spring Cache Abstraction)
- **AI**: Gemini 2.5 Flash API 연동
- **Test**: JUnit5, Mockito (236개 테스트)
- **Build**: Gradle 8

## 폴더 구조
```
INOUT/
├── README.md
└── inout/
    ├── CLAUDE.md                  ← 현재 파일
    ├── build.gradle
    ├── docs/
    │   ├── ARCHITECTURE.md
    │   └── TROUBLESHOOTING.md
    ├── frontend/                  ← React + Vite
    │   ├── src/
    │   │   ├── api/              ← API 모듈 (17개)
    │   │   ├── components/       ← 공통 컴포넌트
    │   │   └── pages/            ← 21개 페이지
    │   └── package.json
    └── src/main/java/com/jstudy/inout/
        ├── common/               ← 인증, JWT, 예외처리, 메일
        ├── order/                ← 발주 (핵심 도메인)
        ├── payment/              ← 결제·예치금
        ├── stock/                ← 재고
        ├── delivery/             ← 배송
        ├── inquiry/              ← 문의게시판
        ├── dashboard/            ← 대시보드
        └── ai/                   ← Gemini AI 연동
```

## 핵심 설계 원칙
1. **동시성**: 비관적 락(PESSIMISTIC_WRITE) + 낙관적 락(@Version) 병행
2. **트랜잭션**: REQUIRES_NEW는 반드시 별도 클래스로 분리 (Spring 프록시 우회 방지)
3. **이벤트**: 메일 발송은 @TransactionalEventListener(AFTER_COMMIT) + @Async로 분리
4. **캐시**: Redis 장애 시 LoggingCacheErrorHandler로 Fallback (DB 직접 조회)
5. **응답 규약**: 모든 API는 ResponseResult → ResponseMessage 래핑 사용

## 자주 쓰는 명령어

### 백엔드 빌드 및 실행
```bash
cd inout
./gradlew bootRun
./gradlew test
./gradlew build
```

### 프론트엔드 실행
```bash
cd inout/frontend
npm install
npm run dev
```

### Git 작업
```bash
git status
git add .
git commit -m "feat: 기능 설명"
git push origin main
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
- Service는 @Transactional(readOnly = true) 기본, 쓰기 메서드만 @Transactional
- 예외는 반드시 InoutException(message, httpStatus, errorCode) 사용
- REQUIRES_NEW가 필요하면 반드시 별도 클래스로 분리

### 주의사항
- application-secret.properties는 git에 올리지 않음 (DB 비밀번호, API 키 포함)
- node_modules는 git 추적 제외 (frontend/.gitignore에 설정됨)
- Redis 없이도 실행 가능 (LoggingCacheErrorHandler Fallback 자동 동작)

## 배포 체크리스트
- [ ] spring.jpa.properties.hibernate.show_sql=false 확인
- [ ] application-secret.properties git 미포함 확인
- [ ] ./gradlew test 전체 통과 확인
- [ ] frontend npm run build 성공 확인
- [ ] node_modules git 미추적 확인
