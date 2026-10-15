# INDEX 글 ↔ 클래스 매핑표 (작성용 작업 문서)

> 티스토리에는 올리지 않는 내부 작업 문서. 각 글을 쓸 때 "이 글에서 공부할 클래스" 체크리스트로 사용한다.
> 패키지 경로는 `com.jstudy.inout` 기준. ★ = 그 글의 주인공 클래스.
> 표 순서 = 독자가 읽는 순서. 실제 공부 순서는 아래 "학습 순서"를 따른다.

## 학습 순서

- [ ] **준비** — 9. 공통 응답 · 예외 계층 · BaseTimeEntity · CustomUserDetails(@AuthenticationPrincipal 원리)
- [ ] **1주차** — 2. order: 장바구니 → 도메인 모델·상태 전이 → 직원 기안 → 점주 결재 → 점주 직접 발주
- [ ] **2주차** — 2. order 본사 승인 ①② → 3. stock → 4. payment → 5. delivery
- [ ] **3주차** — 6. common.auth → jwt → oauth2 → 7. common.config
- [ ] **4주차** — 8. 운영·지원 → **1. 핵심 설계 결정(종합)** → 회고

> 다른 패키지 코드를 만나면 "무엇을 하는지"만 확인하고 넘어간다. 락·트랜잭션 전파·캐시·이벤트를 만나면 1번 표에 메모해 둔다.

---

## 1. 핵심 설계 결정

| 글 | 클래스 / 위치 |
|----|---------------|
| 발주~배송 전체 흐름 | ★OrderEmpService · ★OrderOwnerService · ★OrderAdmService · DepositService · DeliveryService |
| 동시성 제어 | ★ItemRepository.findByIdWithLock · ★OrderRequestRepository(@Lock) · DepositAccountRepository.findByStoreIdForUpdate · UserRepository(@Lock) · DeliveryRepository.findByOrderIdForUpdate · Item(@Version) · GlobalExceptionHandler(낙관적 락 409) |
| REQUIRES_NEW 분리 | ★OrderApprovalTxService · OrderAdmService.bulkApproveOrders · AiCsPersistenceService · AiAutoOrderPersistenceService |
| 이벤트 후처리 | ★OrderStateChangedEvent · ★OrderNotificationEventListener · AsyncConfig · MailComponent |
| Redis 캐시 장애 대응 | ★CacheConfig · ★ResilientCacheManager · ★LoggingCacheErrorHandler · RedisConfig · EmbeddedRedisConfig |
| 공통 응답·예외 | ★ResponseResult · ResponseMessage · ResponseMessageHeader · ResponseError · ★InoutException · ★GlobalExceptionHandler |

---

## 2. order

| 글 | 클래스 |
|----|--------|
| 도메인 모델·상태 전이 | ★OrderRequest · ★OrderDetail · ★OrderStatus · ★OrderDetailStatus · OrderRequestRepository · OrderDetailRepository |
| 장바구니 | ★CartEmpController · ★CartEmpService · Cart · CartDetail · CartRepository · CartDetailRepository · CartAddRequest · CartQuantityUpdateRequest · CartResponse |
| 직원 기안 | ★OrderEmpController · ★OrderEmpService · OrderCreateRequest · OrderPreResponse · OrderItemDto · OrderResponse · OrderListResponse · OrderDetailResponse |
| 점주 결재 | ★OrderOwnerController · ★OrderOwnerService(modifyAndApprove, rejectDraft) · OwnerOrderModifyRequest · OrderRejectRequest · OrderAdminResponse · OrderAdminDetailResponse |
| 점주 직접 발주 | OrderOwnerService.createOwnerOrder · OwnerOrderCreateRequest |
| 본사 승인 ① 단건·항목별 | ★OrderAdmController · ★OrderAdmService · OrderProcessRequest |
| 본사 승인 ② 일괄 | ★OrderApprovalTxService · BulkOrderRequest · BulkOrderResponse |
| 이벤트 알림 | OrderStateChangedEvent · OrderNotificationEventListener · (정리 대상) OrderApprovedEvent · OrderDeliveryEventListener |
| 운영 기능 | OrderAdmController(/excel) · common.config.OrderStatusRenameRunner |
| (미사용 확인 필요) | AiOrderApprovalRequest · AiOrderApprovalResponse |

## 3. stock

| 글 | 클래스 |
|----|--------|
| 도메인 모델 | ★Item · ItemCategory · ★BaseStockHistory(@MappedSuperclass) · StockReceivingHistory · StockUsageHistory · StockType · ItemRepository · ItemCategoryRepository · StockReceivingHistoryRepository · StockUsageHistoryRepository |
| 상품 관리 | ★StockAdmController · ★StockAdmService(register/update/delete) · StockRegister · StockUpdate · ImageController · ImageService · common.util.FileStorageUtil |
| 입고 | StockAdmService.receiveStock · StockReceiveRequest |
| 재고 사용 | ★StockEmpController · ★StockEmpService.useStock · StockUseRequest · NotEnoughStockException |
| 재고 실사 | StockAdmService.adjustStock · StockAdjustRequest · StockAdmRequest |
| 조회·이력·알림 | StockAdminResponse · StockDetailResponse · StockDashboardResponse · StockHistoryResponse · ItemResponse · StockUserDetailResponse |

## 4. payment

| 글 | 클래스 |
|----|--------|
| 도메인 모델 | ★DepositAccount · ★DepositHistory · TransactionType · DepositAccountRepository · DepositHistoryRepository |
| 결제·환불 | ★DepositService(deductDeposit, refundDeposit) · DepositDto · User.consumeDailyDeposit |
| 충전 | ★ChargeService · ChargeRequest · ChargeStatus · ChargeRequestRepository · ChargeDto · DepositOwnerController(/charges) · ChargeAdmController |
| 1일 한도 | OwnerEmployeeController(/deposit-limit) · ★DepositLimitScheduler |
| 조회 | DepositController(/emp/deposit) · DepositOwnerController(/deposit) · DepositEmpService · DepositEmpDto · AdminDepositController · AdminDepositService · AdminDepositDto |
| 레거시·비활성 | PaymentController · PaymentService · PaymentDto · ChargeEmpController · DepositController(/deposit/charge, /deposit/refund) |

## 5. delivery

| 글 | 클래스 |
|----|--------|
| 도메인 모델 | ★Delivery · DeliveryStatus · DeliveryRepository |
| 배송 생성 | ★DeliveryService.createDeliveryIfAbsentForCompletedOrder · markAiInboundWaiting |
| 본사 배송 처리 | ★DeliveryAdmController · DeliveryService(generateWaybill, startShipping, complete) · DeliveryDto |
| 조회·추적 | DeliveryEmpController · DeliveryOwnerController · DeliveryTrackingController · DeliveryTrackingService · DeliveryTrackingDto |
| 자동 완료 | ★DeliveryScheduler · common.config.DeliveryMockWaybillBackfillRunner |

---

## 6. common.auth / oauth2 / jwt

| 글 | 클래스 |
|----|--------|
| 도메인 모델 | ★User · Role · UserRole · UserRoleId · Store · UserStatus · 각 Repository |
| 회원가입 | UserController(/register, /public/check-email) · AuthService · AuthServiceImpl · UserInput |
| 로그인·재발급·로그아웃 | ★AuthLoginController · RefreshToken · RefreshTokenRepository · UserLogin |
| 아이디·비번 찾기 | UserController(/find, /public/password/**, /resetPassword) · UserInputFind · UserPasswordResetInput · PasswordResetCompleteRequest · UserInputPassword |
| 프로필 | UserController(/profile/**) · UserProfileResponse · UserProfileUpdateRequest · UserUpdate |
| 점주 직원 관리 | OwnerUserController · OwnerEmployeeController · OwnerUserDto |
| 본사 회원 관리 | AdminUserController · AdminEmployeeController · AdminUserDto · UserResponse |
| 인증 주체 | ★CustomUserDetails · CustomUserDetailsService · UserDisplayNames |
| OAuth2 로그인 | ★CustomOAuth2UserService · OAuthAttributes · CustomOAuth2UserDetails · OAuth2AuthenticationSuccessHandler · OAuth2AuthenticationFailureHandler |
| 소셜 온보딩 | SocialOnboardingController · SocialOnboardingService · SocialOnboardingRequest · StoreService |
| JWT | ★JwtTokenProvider · JwtToken · ★JwtAuthenticationFilter(auth.filter) |

## 7. common.config

| 글 | 클래스 |
|----|--------|
| Security | ★SecurityConfig · CustomAuthenticationEntryPoint · CustomAccessDeniedHandler |
| 캐시·Redis | (1. 핵심 설계 결정과 공유) |
| 비동기·Auditing·Web·Swagger | AsyncConfig · JpaAuditConfig · WebConfig · SwaggerConfig |
| 로깅 | MdcLoggingFilter · resources/logback-spring.xml |
| 마이그레이션 러너 | RoleInitRunner · OrderStatusRenameRunner · UserStatusLeaveRenameRunner · InquiryTargetTypeRunner · ResignedStoreDetachRunner · DeliveryMockWaybillBackfillRunner |
| 더미 데이터 | DummyDataController · DummyDataInitializer · DummyDataScheduler · DummyDataService · AdminViewController |

## 8. 운영·지원

| 패키지 | 글 | 클래스 |
|--------|----|--------|
| dashboard | 역할별 집계 | DashboardController · DashboardEmpController · DashboardOwnerController · DashboardService · DashboardEmpService · DashboardOwnerService · 4개 Response DTO |
| dashboard | 캐싱 | ★DashboardAggregateService |
| inquiry | 작성·상세·삭제 | ★InquiryController · ★InquiryService · Inquiry · InquiryRepository · InquiryCreateRequest · InquiryDetailResponse |
| inquiry | 대상별 목록 | InquiryTargetType · InquiryListResponse |
| inquiry | 댓글 | InquiryCommentController · InquiryCommentService · InquiryComment · InquiryCommentRepository · CommentCreateRequest · CommentUpdateRequest · CommentResponse |
| leave | 신청 | AnnualLeaveEmpController · ★AnnualLeaveService · AnnualLeave · LeaveType · AnnualLeaveRepository · AnnualLeaveDto |
| leave | 결재 | AnnualLeaveOwnerController · AnnualLeaveAdmController · LeaveStatus |
| ai | 인사이트 | AiInsightController · ★AiInsightService · AiInsightResponse |
| ai | 자동 발주 | ★AiAutoOrderService · AiAutoOrderScheduler · AiAutoOrderPersistenceService |
| ai | CS 분류 | ★AiCsService · AiCsScheduler · AiCsPersistenceService |
| ai | 스마트 추천 | StockEmpService.getAiStockSuggestions · AiStockSuggestionResponse |
| common.mail | 템플릿·발송 | MailComponent · MailTemplate · MailTemplateRepository · MailTemplateCacheService |

## 9. 공통 기반

| 글 | 클래스 |
|----|--------|
| 공통 응답 | ResponseResult · ResponseMessage · ResponseMessageHeader · ResponseError · ServiceResult |
| 예외 계층 | InoutException · UserNotFoundException · (stock) NotEnoughStockException |
| 기반 엔티티 | BaseTimeEntity · JpaAuditConfig |
| 대용량 데이터 | MassDataController · MassDataGeneratorService · MassDataGenerationResponse · MassDataStepResult |

---

## 학습 중 확인할 개선 포인트 (회고 글 재료)

- [ ] `OrderApprovedEvent`를 발행하는 코드가 없음 → `OrderDeliveryEventListener`는 실행되지 않는 리스너. 배송 생성은 서비스에서 직접 호출 중. 제거할지 이벤트로 통일할지 결정
- [ ] `AiOrderApprovalRequest/Response` 사용처 없음
- [ ] `DeliveryEmpController`, `AnnualLeaveEmpController`의 `@Operation` 한글이 `???`로 깨져 있음 (파일 인코딩 손실)
- [ ] `CartEmpService.deleteSelectedCartItems`에 본인 소유 검증 누락 의심
- [ ] `docs/ARCHITECTURE.md`가 레거시 상태(PAID/COMPLETED)·비활성 PaymentService 기준으로 서술됨 → 현재 흐름(REQUESTED→ORDERED→APPROVED)으로 갱신 필요
- [x] 미사용 공통 클래스 삭제 — ErrorResponse · AuthFailException · ResourceNotFoundException · UnauthorizedException, ResponseResult.result() · ServiceResult.result() (2026-10-09)
- [ ] `resultCode`가 문자열 리터럴로 흩어져 있음(`"ORDER_NOT_FOUND"` 등) → `ErrorCode` Enum으로 모으면 오타 방지·목록화 가능
- [x] 비밀번호 재설정 실패를 ServiceResult.fail → InoutException(404 USER_NOT_FOUND / 400 EXPIRED_LINK)으로 통일, 컨트롤러 try/catch 제거 (2026-10-09)
- [x] 재설정 링크 만료를 BaseTimeEntity.updatedAt → passwordResetLimit으로 판단 (2026-10-09)
- [x] UserNotFoundException이 InoutException을 상속(404, USER_NOT_FOUND), 전용 핸들러 제거 (2026-10-09)
- [x] 인자 1개 InoutException 6곳에 status·resultCode 지정, 재고 부족 코드 STOCK_SHORTAGE로 통일 (2026-10-09)
- [x] 예치금 금액 검증 IllegalArgumentException(500) → InoutException(400 INVALID_AMOUNT) + DTO @Valid (2026-10-09)
- [x] ResponseError의 FieldError 강제 캐스팅 제거, DummyDataController ADMIN 권한 추가 (2026-10-09)
- [x] @PreAuthorize(메서드 보안) 거부가 GlobalExceptionHandler의 Exception 핸들러에 잡혀 500으로 응답 → AccessDeniedException 핸들러 추가로 403 AUTH_403 통일, ACL 회귀 테스트 3개 추가 (2026-10-10)
- [x] 프론트 실패 토스트 단일화: 공통 인터셉터만 토스트를 띄우고 화면 catch 34곳의 중복 토스트 제거, 검증 오류는 첫 필드 오류를 토스트로·입력칸별 표시(온보딩·직원 생성), blob 다운로드 오류 문구 파싱 (2026-10-15)
- [x] 실패 문구 정비 (2026-10-15): A 내부 정보 노출·깨진 문구 18건+AI 공급자명 4건 수정(원인은 log로), B 조회 API 48곳에 errorContext(5xx·네트워크 시 화면 문맥), C 표준 문구·resultCode 79건 통일(발주 용어, *_NOT_FOUND, NOT_OWNER, CROSS_STORE_FORBIDDEN, 중복·이미 처리 409) — 규칙은 ERROR_MESSAGE_GUIDE.md
- [x] D: ErrorCode Enum 도입 (2026-10-15) — InoutException 문자열 생성자 삭제, 호출부 219곳·핸들러·Security 핸들러 전환, 로그인 실패 resultCode null 해결, VALIDATION_ERROR 추가, ErrorCodeTest로 문구 규칙 검사
- [ ] E: resultCode 기반 화면 분기 (INSUFFICIENT_BALANCE, STOCK_SHORTAGE, CONCURRENCY_ERROR 등)
