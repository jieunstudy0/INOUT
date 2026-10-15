# 백엔드 실패 문구 전수 목록 (2026-10-15 정비 후, 224건)

> ErrorCode 도입(D단계) 이전의 스냅샷이다. 현재 기준은 `common/exception/ErrorCode.java`.
> 자동 추출. '점검' 열은 [ERROR_MESSAGE_GUIDE.md](ERROR_MESSAGE_GUIDE.md) 규칙 위반 후보.

| 위치 | status | resultCode | 문구 | 점검 |
|---|---|---|---|---|
| `ai/controller/AiInsightController.java:60` | 504 | AI_TIMEOUT | "AI 분석 요청이 시간 초과되었습니다 (" + TIMEOUT_SECONDS + "초). 잠시 후 다시 시도해 주세요." |  |
| `ai/controller/AiInsightController.java:68` | 500 | AI_EXECUTION_ERROR | "AI 분석 처리 중 내부 오류가 발생했습니다." |  |
| `ai/controller/AiInsightController.java:72` | 500 | AI_INTERRUPTED | "AI 분석 요청이 중단되었습니다." |  |
| `ai/service/AiAutoOrderPersistenceService.java:65` | 500 | ADMIN_NOT_FOUND | "시스템 관리자 계정이 없어 자동 발주를 처리할 수 없습니다. 관리자에게 문의해 주세요." |  |
| `ai/service/AiAutoOrderService.java:169` | 500 | AI_INVALID_RESPONSE | "AI 응답 형식이 올바르지 않습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiAutoOrderService.java:184` | 503 | AI_API_ERROR | "AI 서비스 호출에 실패했습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiAutoOrderService.java:193` | 503 | AI_API_ERROR | "AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiAutoOrderService.java:200` | 500 | AI_PARSE_ERROR | "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiAutoOrderService.java:206` | 503 | AI_API_ERROR | "AI 서비스 호출에 실패했습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiAutoOrderService.java:292` | 500 | AI_PARSE_ERROR | "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiAutoOrderService.java:302` | 500 | AI_DESERIALIZE_ERROR | "AI 추천 데이터 파싱에 실패했습니다." |  |
| `ai/service/AiAutoOrderService.java:76` | 503 | AI_NOT_CONFIGURED | "AI 서비스가 설정되지 않아 사용할 수 없습니다. 관리자에게 문의해 주세요." |  |
| `ai/service/AiCsService.java:147` | 404 | INQUIRY_NOT_FOUND | "문의글을 찾을 수 없습니다." |  |
| `ai/service/AiCsService.java:150` | 409 | AI_DRAFT_ALREADY_EXISTS | "이미 AI 초안이 생성된 문의입니다." |  |
| `ai/service/AiCsService.java:272` | 500 | AI_INVALID_RESPONSE | "AI 응답 형식이 올바르지 않습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiCsService.java:278` | 503 | AI_API_ERROR | "AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiCsService.java:283` | 500 | AI_RESPONSE_ERROR | "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요." |  |
| `ai/service/AiCsService.java:419` | 503 | AI_NOT_CONFIGURED | "AI 서비스가 설정되지 않아 사용할 수 없습니다. 관리자에게 문의해 주세요." |  |
| `ai/service/AiInsightService.java:165` | 500 | AI_INVALID_RESPONSE | "AI 응답 형식이 올바르지 않습니다." |  |
| `ai/service/AiInsightService.java:172` | 503 | AI_API_ERROR | "AI 서비스 연결에 실패했습니다." |  |
| `ai/service/AiInsightService.java:175` | 500 | AI_PARSE_ERROR | "AI 응답 처리 중 오류가 발생했습니다." |  |
| `ai/service/AiInsightService.java:48` | 503 | AI_NOT_CONFIGURED | "AI 서비스가 설정되지 않아 사용할 수 없습니다. 관리자에게 문의해 주세요." |  |
| `ai/service/AiInsightService.java:72` | 500 | AI_SERVICE_ERROR | "AI 분석 중 오류가 발생했습니다." |  |
| `common/auth/controller/AuthLoginController.java:106` | 403 | ACCOUNT_LOCKED | "계정이 잠겼습니다. 관리자에게 문의해주세요." |  |
| `common/auth/controller/AuthLoginController.java:109` | 403 | ACCOUNT_DISABLED | e.getMessage() != null ? e.getMessage() : "비활성 계정입니다. 로그인이 불가합니다.", 403, "ACCOUNT_DISABLED" |  |
| `common/auth/controller/AuthLoginController.java:119` | 401 | UNAUTHORIZED | "이메일 또는 비밀번호가 잘못되었거나, 5회 이상 실패하여 계정이 잠겼습니다." |  |
| `common/auth/controller/AuthLoginController.java:136` | 400 | BAD_REQUEST | "리프레시 토큰이 필요합니다." |  |
| `common/auth/controller/AuthLoginController.java:139` | 401 | UNAUTHORIZED | "유효하지 않거나 만료된 리프레시 토큰입니다." |  |
| `common/auth/controller/AuthLoginController.java:143` | 401 | UNAUTHORIZED | "유효하지 않은 토큰입니다." |  |
| `common/auth/controller/AuthLoginController.java:146` | 401 | TOKEN_EXPIRED | "만료된 토큰입니다. 다시 로그인해주세요." |  |
| `common/auth/controller/OwnerEmployeeController.java:44` | 401 | UNAUTHORIZED | "인증 정보가 유효하지 않습니다." |  |
| `common/auth/controller/OwnerEmployeeController.java:59` | 401 | UNAUTHORIZED | "인증 정보가 유효하지 않습니다." |  |
| `common/auth/controller/OwnerUserController.java:103` | 401 | UNAUTHORIZED | "인증 정보가 유효하지 않습니다." |  |
| `common/auth/controller/UserController.java:118` | 400 | INVALID_LINK | "유효하지 않은 재설정 링크입니다." |  |
| `common/auth/controller/UserController.java:160` | - | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/controller/UserController.java:206` | - | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/controller/UserController.java:78` | - | USER_NOT_FOUND | "입력하신 정보와 일치하는 이메일을 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:107` | 409 | DUPLICATE_EMAIL | "이미 사용 중인 이메일입니다." |  |
| `common/auth/service/AuthServiceImpl.java:114` | - | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:121` | - | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:126` | 404 | STORE_NOT_FOUND | "입력하신 매장명을 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:143` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:149` | 500 | MAIL_TEMPLATE_NOT_FOUND | "메일 템플릿이 존재하지 않습니다." |  |
| `common/auth/service/AuthServiceImpl.java:166` | 400 | INVALID_LINK | "유효하지 않은 재설정 링크입니다." |  |
| `common/auth/service/AuthServiceImpl.java:171` | 400 | EXPIRED_LINK | "링크 유효 시간(30분)이 만료되었습니다. 다시 시도해 주세요." |  |
| `common/auth/service/AuthServiceImpl.java:185` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:187` | 404 | STORE_NOT_FOUND | "매장 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:208` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:259` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:264` | 404 | STORE_NOT_FOUND | "매장 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:269` | 500 | ROLE_NOT_FOUND | "관리자 권한 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:290` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:346` | 400 | PASSWORD_CONFIRM_MISMATCH | "비밀번호 확인이 일치하지 않습니다." |  |
| `common/auth/service/AuthServiceImpl.java:349` | 409 | DUPLICATE_EMAIL | "이미 사용 중인 이메일입니다." |  |
| `common/auth/service/AuthServiceImpl.java:364` | 500 | ROLE_NOT_FOUND | "기본 권한 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:374` | 400 | STATUS_REQUIRED | "변경할 계정 상태를 선택해 주세요." |  |
| `common/auth/service/AuthServiceImpl.java:390` | 400 | INVALID_DAILY_DEPOSIT_LIMIT | e.getMessage(), 400, "INVALID_DAILY_DEPOSIT_LIMIT" |  |
| `common/auth/service/AuthServiceImpl.java:417` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:419` | 403 | STORE_REQUIRED | "소속 매장 정보가 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:426` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:428` | 403 | CROSS_STORE_FORBIDDEN | "소속 매장의 직원만 관리할 수 있습니다." |  |
| `common/auth/service/AuthServiceImpl.java:63` | 409 | DUPLICATE_EMAIL | "이미 사용 중인 이메일입니다." |  |
| `common/auth/service/AuthServiceImpl.java:67` | 404 | STORE_NOT_FOUND | "매장 정보를 찾을 수 없습니다." |  |
| `common/auth/service/AuthServiceImpl.java:82` | 500 | ROLE_NOT_FOUND | "기본 권한 정보를 찾을 수 없습니다." |  |
| `common/auth/service/SocialOnboardingService.java:52` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `common/auth/service/SocialOnboardingService.java:57` | 409 | ALREADY_COMPLETED | "이미 온보딩이 완료된 계정입니다." |  |
| `common/auth/service/SocialOnboardingService.java:70` | 500 | ROLE_NOT_FOUND | "회원 권한 정보를 찾을 수 없습니다. 관리자에게 문의해 주세요." |  |
| `common/mail/MailTemplateCacheService.java:22` | 500 | MAIL_TEMPLATE_NOT_FOUND | "메일 템플릿이 존재하지 않습니다." |  |
| `common/massdata/MassDataGeneratorService.java:217` | 500 | MASS_DATA_SCHEMA_ERROR | "더미 데이터 생성에 필요한 테이블 정보를 조회하지 못했습니다." |  |
| `common/massdata/MassDataGeneratorService.java:83` | 400 | INVALID_SCALE | "scale은 " + MIN_SCALE + " 이상 " + MAX_SCALE + " 이하만 허용됩니다 (서버 리소스 보호를 위한 상한). 입력값: " + scale |  |
| `common/util/FileStorageUtil.java:22` | 400 | INVALID_FILE_NAME | "파일 이름이 유효하지 않습니다." |  |
| `common/util/FileStorageUtil.java:28` | 400 | INVALID_FILE_NAME | "허용되지 않는 파일 이름입니다." |  |
| `common/util/FileStorageUtil.java:33` | 400 | INVALID_FILE_EXTENSION | "확장자가 없는 파일은 업로드할 수 없습니다." |  |
| `common/util/FileStorageUtil.java:38` | 400 | INVALID_FILE_EXTENSION | "허용되지 않는 파일 형식입니다. 허용된 확장자: " + allowedExtensions |  |
| `common/util/FileStorageUtil.java:49` | 400 | INVALID_FILE_PATH | "잘못된 업로드 경로입니다." |  |
| `dashboard/service/DashboardOwnerService.java:34` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `dashboard/service/DashboardOwnerService.java:37` | 403 | STORE_REQUIRED | "소속 매장 정보가 없습니다." |  |
| `delivery/controller/DeliveryEmpController.java:43` | 401 | UNAUTHORIZED | "인증 정보가 유효하지 않습니다." |  |
| `delivery/controller/DeliveryOwnerController.java:48` | 403 | STORE_REQUIRED | "소속 매장 정보가 없습니다." |  |
| `delivery/service/DeliveryService.java:101` | 400 | INVALID_DELIVERY_STATUS | "배송 준비 상태에서만 운송장 발급이 가능합니다." |  |
| `delivery/service/DeliveryService.java:125` | 404 | DELIVERY_NOT_FOUND | "배송 정보를 찾을 수 없습니다." |  |
| `delivery/service/DeliveryService.java:168` | 400 | INVALID_TRACKING_NUMBER | "운송장 번호는 필수입니다." |  |
| `delivery/service/DeliveryService.java:175` | 404 | DELIVERY_NOT_FOUND | "배송 정보를 찾을 수 없습니다." |  |
| `delivery/service/DeliveryService.java:178` | 400 | INVALID_DELIVERY_STATUS | "배송 준비 상태에서만 배송 시작이 가능합니다." |  |
| `delivery/service/DeliveryService.java:194` | 404 | DELIVERY_NOT_FOUND | "배송 정보를 찾을 수 없습니다." |  |
| `delivery/service/DeliveryService.java:197` | 400 | INVALID_DELIVERY_STATUS | "배송 중 상태에서만 배송 완료 처리가 가능합니다." |  |
| `delivery/service/DeliveryService.java:45` | 400 | ORDER_SHIPPING_SNAPSHOT_MISSING | "발주에 저장된 배송 정보가 없습니다." |  |
| `delivery/service/DeliveryService.java:80` | 404 | DELIVERY_NOT_FOUND | "배송 정보를 찾을 수 없습니다." |  |
| `delivery/service/DeliveryService.java:94` | 500 | WAYBILL_INTERRUPTED | "운송장 발급이 중단되었습니다." |  |
| `delivery/service/DeliveryService.java:98` | 404 | DELIVERY_NOT_FOUND | "배송 정보를 찾을 수 없습니다." |  |
| `inquiry/service/InquiryCommentService.java:28` | 404 | INQUIRY_NOT_FOUND | "문의글을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryCommentService.java:31` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `inquiry/service/InquiryCommentService.java:36` | 404 | COMMENT_NOT_FOUND | "부모 댓글을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryCommentService.java:39` | 400 | INVALID_COMMENT_DEPTH | "답댓글에는 답댓글을 달 수 없습니다." |  |
| `inquiry/service/InquiryCommentService.java:56` | 404 | COMMENT_NOT_FOUND | "댓글을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryCommentService.java:59` | 403 | NOT_OWNER | "본인이 작성한 댓글만 삭제할 수 있습니다." |  |
| `inquiry/service/InquiryCommentService.java:68` | 404 | COMMENT_NOT_FOUND | "댓글을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryCommentService.java:71` | 403 | NOT_OWNER | "본인이 작성한 댓글만 수정할 수 있습니다." |  |
| `inquiry/service/InquiryService.java:144` | 404 | INQUIRY_NOT_FOUND | "문의글을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryService.java:152` | 403 | FORBIDDEN | "조회 권한이 없습니다." |  |
| `inquiry/service/InquiryService.java:165` | 404 | INQUIRY_NOT_FOUND | "문의글을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryService.java:168` | 403 | NOT_OWNER | "본인이 작성한 문의글만 삭제할 수 있습니다." |  |
| `inquiry/service/InquiryService.java:180` | 404 | INQUIRY_NOT_FOUND | "문의글을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryService.java:183` | 403 | FORBIDDEN | "다운로드 권한이 없습니다." |  |
| `inquiry/service/InquiryService.java:190` | 404 | FILE_NOT_FOUND | "첨부파일이 존재하지 않습니다." |  |
| `inquiry/service/InquiryService.java:198` | 404 | FILE_NOT_FOUND | "첨부파일을 찾을 수 없습니다." |  |
| `inquiry/service/InquiryService.java:209` | 500 | FILE_DOWNLOAD_ERROR | "첨부파일 다운로드 중 오류가 발생했습니다." |  |
| `inquiry/service/InquiryService.java:212` | 500 | FILE_DOWNLOAD_ERROR | "첨부파일 다운로드 중 오류가 발생했습니다." |  |
| `inquiry/service/InquiryService.java:222` | 400 | INVALID_FILE_PATH | "잘못된 파일 경로입니다." |  |
| `inquiry/service/InquiryService.java:44` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `inquiry/service/InquiryService.java:76` | 500 | FILE_UPLOAD_ERROR | "파일 업로드 중 오류가 발생했습니다." |  |
| `leave/controller/AnnualLeaveEmpController.java:89` | 401 | UNAUTHORIZED | "인증 정보가 유효하지 않습니다." |  |
| `leave/controller/AnnualLeaveOwnerController.java:90` | 403 | STORE_REQUIRED | "소속 매장 정보가 없습니다." |  |
| `leave/controller/AnnualLeaveOwnerController.java:97` | 401 | UNAUTHORIZED | "인증 정보가 유효하지 않습니다." |  |
| `leave/service/AnnualLeaveService.java:104` | 404 | LEAVE_NOT_FOUND | "연차 신청 정보를 찾을 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:107` | 404 | PROCESSOR_NOT_FOUND | "처리자 정보를 찾을 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:110` | 403 | STORE_REQUIRED | "소속 매장이 없는 계정은 연차를 처리할 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:115` | 409 | ALREADY_PROCESSED | "이미 처리된 연차 신청입니다." |  |
| `leave/service/AnnualLeaveService.java:122` | 400 | REJECT_REASON_REQUIRED | "반려 사유를 입력해야 합니다." |  |
| `leave/service/AnnualLeaveService.java:130` | 400 | INVALID_LEAVE_STATUS | "허용되지 않은 처리 상태입니다." |  |
| `leave/service/AnnualLeaveService.java:156` | 403 | CROSS_STORE_FORBIDDEN | "다른 매장 직원의 연차에 접근할 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:34` | 400 | INVALID_LEAVE_PERIOD | "시작일은 종료일보다 늦을 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:38` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:43` | 409 | LEAVE_PERIOD_OVERLAP | "이미 신청된 연차 기간과 겹칩니다." |  |
| `leave/service/AnnualLeaveService.java:63` | 404 | LEAVE_NOT_FOUND | "연차 신청 정보를 찾을 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:66` | 403 | NOT_OWNER | "본인의 연차 신청만 조회할 수 있습니다." |  |
| `leave/service/AnnualLeaveService.java:81` | 404 | LEAVE_NOT_FOUND | "연차 신청 정보를 찾을 수 없습니다." |  |
| `leave/service/AnnualLeaveService.java:94` | 404 | LEAVE_NOT_FOUND | "연차 신청 정보를 찾을 수 없습니다." |  |
| `order/controller/OrderAdmController.java:149` | 500 | EXCEL_ERROR | "엑셀 파일 생성 중 오류가 발생했습니다." |  |
| `order/controller/OrderEmpController.java:127` | 401 | UNAUTHORIZED | "인증 정보가 유효하지 않습니다." |  |
| `order/service/CartEmpService.java:124` | 404 | CART_ITEM_NOT_FOUND | "장바구니 항목을 찾을 수 없습니다." |  |
| `order/service/CartEmpService.java:127` | 403 | NOT_OWNER | "본인의 장바구니 항목만 수정할 수 있습니다." |  |
| `order/service/CartEmpService.java:40` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `order/service/CartEmpService.java:49` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/CartEmpService.java:52` | 403 | NOT_OWNER | "본인의 발주 내역만 재주문할 수 있습니다." |  |
| `order/service/CartEmpService.java:70` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:106` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:149` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:151` | 404 | ADMIN_NOT_FOUND | "관리자 정보를 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:156` | 400 | NOT_PAID_ORDER | "결제가 완료되지 않은 발주 건입니다." |  |
| `order/service/OrderAdmService.java:162` | 400 | EMPTY_ORDER_ITEMS | "처리할 발주 상세 항목이 없습니다." |  |
| `order/service/OrderAdmService.java:168` | 404 | ORDER_DETAIL_NOT_FOUND | "해당 발주에 속한 상세 항목을 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:177` | 409 | ALREADY_PROCESSED_DETAIL | "이미 처리된 발주 상세 항목은 상태를 변경할 수 없습니다." |  |
| `order/service/OrderAdmService.java:181` | 400 | WAITING_ROLLBACK_FORBIDDEN | "대기 상태로 되돌릴 수 없습니다." |  |
| `order/service/OrderAdmService.java:237` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:240` | 400 | INVALID_ORDER_STATUS | "본사 승인 대기 상태의 발주만 승인할 수 있습니다." |  |
| `order/service/OrderAdmService.java:244` | 400 | STOCK_SHORTAGE | "재고 부족으로 승인이 거부되었습니다. 발주가 자동 반려·환불 처리되었습니다." |  |
| `order/service/OrderAdmService.java:253` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:255` | 400 | INVALID_ORDER_STATUS | "본사 승인 대기 상태의 발주만 반려할 수 있습니다." |  |
| `order/service/OrderAdmService.java:265` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `order/service/OrderAdmService.java:277` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `order/service/OrderApprovalTxService.java:139` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderApprovalTxService.java:52` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderApprovalTxService.java:54` | 404 | ADMIN_NOT_FOUND | "관리자 정보를 찾을 수 없습니다." |  |
| `order/service/OrderApprovalTxService.java:80` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `order/service/OrderEmpService.java:146` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderEmpService.java:149` | 403 | NOT_OWNER | "본인의 발주만 조회할 수 있습니다." |  |
| `order/service/OrderEmpService.java:179` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderEmpService.java:182` | 403 | NOT_OWNER | "본인의 발주만 취소할 수 있습니다." |  |
| `order/service/OrderEmpService.java:202` | 400 | INVALID_ORDER_STATUS | "이미 처리 중이거나 완료된 발주는 취소할 수 없습니다." |  |
| `order/service/OrderEmpService.java:49` | 400 | EMPTY_SELECTION | "선택된 상품이 없습니다." |  |
| `order/service/OrderEmpService.java:56` | 403 | NOT_OWNER | "본인의 장바구니 상품만 발주할 수 있습니다." |  |
| `order/service/OrderEmpService.java:66` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `order/service/OrderEmpService.java:71` | 400 | EMPTY_ORDER | "발주할 상품이 없습니다." |  |
| `order/service/OrderEmpService.java:76` | 403 | NOT_OWNER | "본인의 장바구니 상품만 발주할 수 있습니다." |  |
| `order/service/OrderEmpService.java:80` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `order/service/OrderEmpService.java:83` | 400 | STOCK_SHORTAGE | item.getName() + " 재고가 부족합니다.", 400, "STOCK_SHORTAGE" |  |
| `order/service/OrderOwnerService.java:102` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:115` | 400 | EMPTY_ORDER_ITEMS | "결제할 품목이 없습니다." |  |
| `order/service/OrderOwnerService.java:125` | 404 | USER_NOT_FOUND | "신청자 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:129` | 400 | DAILY_DEPOSIT_LIMIT_EXCEEDED | "1일 예치금 사용 한도를 초과했습니다." |  |
| `order/service/OrderOwnerService.java:152` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:156` | 400 | INVALID_ORDER_STATUS | "직원 기안(승인 대기) 상태의 발주만 반려할 수 있습니다." |  |
| `order/service/OrderOwnerService.java:178` | 400 | EMPTY_ORDER | "발주할 품목이 없습니다." |  |
| `order/service/OrderOwnerService.java:184` | 400 | INVALID_ITEM_LINE | "품목 ID와 수량(1 이상)이 필요합니다." |  |
| `order/service/OrderOwnerService.java:193` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:195` | 400 | STOCK_SHORTAGE | item.getName() + " 재고가 부족합니다.", 400, "STOCK_SHORTAGE" |  |
| `order/service/OrderOwnerService.java:202` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:206` | 400 | DAILY_DEPOSIT_LIMIT_EXCEEDED | "1일 예치금 사용 한도를 초과했습니다." |  |
| `order/service/OrderOwnerService.java:247` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:250` | 403 | CROSS_STORE_FORBIDDEN | "소속 매장의 발주만 조회할 수 있습니다." |  |
| `order/service/OrderOwnerService.java:258` | 403 | CROSS_STORE_FORBIDDEN | "소속 매장의 발주만 처리할 수 있습니다." |  |
| `order/service/OrderOwnerService.java:313` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:315` | 403 | STORE_REQUIRED | "소속 매장 정보가 없습니다." |  |
| `order/service/OrderOwnerService.java:68` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `order/service/OrderOwnerService.java:72` | 400 | INVALID_ORDER_STATUS | "직원 기안(승인 대기) 상태의 발주만 수정·결제할 수 있습니다." |  |
| `order/service/OrderOwnerService.java:75` | 400 | EMPTY_ORDER_ITEMS | "결제할 품목이 없습니다." |  |
| `order/service/OrderOwnerService.java:81` | 400 | INVALID_ITEM_LINE | "품목 ID와 수량(1 이상)이 필요합니다." |  |
| `payment/controller/DepositOwnerController.java:67` | 403 | STORE_REQUIRED | "소속 매장 정보가 없습니다." |  |
| `payment/service/ChargeService.java:28` | 400 | INVALID_AMOUNT | "충전 금액은 0보다 커야 합니다." |  |
| `payment/service/ChargeService.java:31` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `payment/service/ChargeService.java:44` | 404 | ADMIN_NOT_FOUND | "관리자 정보를 찾을 수 없습니다." |  |
| `payment/service/ChargeService.java:47` | 404 | REQUEST_NOT_FOUND | "충전 요청을 찾을 수 없습니다." |  |
| `payment/service/ChargeService.java:50` | 409 | ALREADY_PROCESSED | "이미 처리된 요청입니다." |  |
| `payment/service/ChargeService.java:66` | 404 | ADMIN_NOT_FOUND | "관리자 정보를 찾을 수 없습니다." |  |
| `payment/service/ChargeService.java:69` | 404 | REQUEST_NOT_FOUND | "충전 요청을 찾을 수 없습니다." |  |
| `payment/service/ChargeService.java:72` | 409 | ALREADY_PROCESSED | "이미 처리된 요청입니다." |  |
| `payment/service/DepositService.java:107` | 403 | STORE_REQUIRED | "소속 매장이 없는 사용자는 예치금을 사용할 수 없습니다." |  |
| `payment/service/DepositService.java:111` | 404 | ACCOUNT_NOT_FOUND | "예치금 계좌를 찾을 수 없습니다." |  |
| `payment/service/DepositService.java:116` | 400 | INSUFFICIENT_BALANCE | "예치금 잔액이 부족합니다. 충전 후 다시 시도해 주세요." |  |
| `payment/service/DepositService.java:59` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `payment/service/DepositService.java:64` | 403 | STORE_REQUIRED | "소속 매장이 없는 사용자는 예치금을 사용할 수 없습니다." |  |
| `payment/service/DepositService.java:94` | 400 | INVALID_AMOUNT | "금액은 0보다 커야 합니다." |  |
| `payment/service/PaymentService.java:26` | 404 | ORDER_NOT_FOUND | "발주 정보를 찾을 수 없습니다." |  |
| `payment/service/PaymentService.java:29` | 403 | NOT_OWNER | "본인의 발주만 결제할 수 있습니다." |  |
| `payment/service/PaymentService.java:33` | 403 | OWNER_APPROVAL_REQUIRED | "직원 기안은 점주가 승인·결제한 뒤에만 본사로 전달됩니다. 예치금 결제는 점주 화면에서 진행해 주세요." |  |
| `payment/service/PaymentService.java:39` | 400 | INVALID_ORDER_STATUS | "결제 대기 상태의 발주가 아닙니다." |  |
| `stock/exception/NotEnoughStockException.java:12` | - | STOCK_SHORTAGE | String.format("재고가 부족합니다. (현재: %d, 요청: %d)", current, request |  |
| `stock/service/ImageService.java:24` | 400 | FILE_EMPTY | "업로드할 파일이 없습니다." |  |
| `stock/service/ImageService.java:45` | 500 | FILE_UPLOAD_ERROR | "이미지 저장 중 오류가 발생했습니다." |  |
| `stock/service/StockAdmService.java:114` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:117` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:171` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:206` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:209` | 404 | ADMIN_NOT_FOUND | "관리자 정보를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:49` | 404 | CATEGORY_NOT_FOUND | "해당 카테고리를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:52` | 409 | DUPLICATE_ITEM_NAME | "이미 등록된 상품명입니다." |  |
| `stock/service/StockAdmService.java:65` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:68` | 404 | CATEGORY_NOT_FOUND | "카테고리를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:83` | 404 | ITEM_NOT_FOUND | "상품 정보를 찾을 수 없습니다." |  |
| `stock/service/StockAdmService.java:86` | 409 | ALREADY_DELETED | "이미 삭제된 상품입니다." |  |
| `stock/service/StockEmpService.java:45` | 404 | ITEM_NOT_FOUND | "사용 가능한 상품이 없습니다." |  |
| `stock/service/StockEmpService.java:48` | 404 | USER_NOT_FOUND | "사용자 정보를 찾을 수 없습니다." |  |
| `stock/service/StockEmpService.java:82` | 404 | ITEM_NOT_FOUND | "존재하지 않거나 삭제된 상품입니다." |  |
