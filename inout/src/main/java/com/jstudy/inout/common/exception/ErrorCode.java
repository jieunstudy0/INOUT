package com.jstudy.inout.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 실패 응답의 resultCode·HTTP 상태·기본 문구를 한곳에 정의한다.
 * 상수 이름이 그대로 resultCode가 되며, 문구 작성 규칙은 docs/study/ERROR_MESSAGE_GUIDE.md를 따른다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    ALREADY_PROCESSED(409, "이미 처리된 요청입니다."),
    CONCURRENCY_ERROR(409, "이미 다른 요청에 의해 처리되었습니다. 새로고침 후 다시 시도해주세요."),
    CROSS_STORE_FORBIDDEN(403, "소속 매장의 정보만 처리할 수 있습니다."),
    FORBIDDEN(403, "해당 작업을 수행할 권한이 없습니다."),
    INTERNAL_ERROR(500, "서버 내부에서 예상치 못한 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_SCALE(400, "허용되지 않는 생성 배율입니다."),
    MAIL_TEMPLATE_NOT_FOUND(500, "메일 템플릿이 존재하지 않습니다."),
    MASS_DATA_SCHEMA_ERROR(500, "더미 데이터 생성에 필요한 테이블 정보를 조회하지 못했습니다."),
    NOT_OWNER(403, "본인의 정보만 처리할 수 있습니다."),
    VALIDATION_ERROR(400, "입력 데이터에 유효성 오류가 있습니다."),

    // 회원·인증
    ACCOUNT_DISABLED(403, "비활성 계정입니다. 로그인이 불가합니다."),
    ACCOUNT_LOCKED(403, "계정이 잠겼습니다. 관리자에게 문의해주세요."),
    ADMIN_NOT_FOUND(404, "관리자 정보를 찾을 수 없습니다."),
    ALREADY_COMPLETED(409, "이미 온보딩이 완료된 계정입니다."),
    AUTH_401(401, "인증 정보가 유효하지 않습니다. 로그인이 필요합니다."),
    AUTH_403(403, "접근 권한이 없습니다."),
    CURRENT_PASSWORD_MISMATCH(400, "현재 비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),
    EXPIRED_LINK(400, "링크 유효 시간(30분)이 만료되었습니다. 다시 시도해 주세요."),
    INVALID_LINK(400, "유효하지 않은 재설정 링크입니다."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 일치하지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(400, "비밀번호 확인이 일치하지 않습니다."),
    PASSWORD_MISMATCH(400, "비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_REQUIRED(400, "리프레시 토큰이 필요합니다."),
    ROLE_NOT_FOUND(500, "권한 정보를 찾을 수 없습니다. 관리자에게 문의해 주세요."),
    STATUS_REQUIRED(400, "변경할 계정 상태를 선택해 주세요."),
    STORE_NOT_FOUND(404, "매장 정보를 찾을 수 없습니다."),
    STORE_REQUIRED(403, "소속 매장 정보가 없습니다."),
    TOKEN_EXPIRED(401, "만료된 토큰입니다. 다시 로그인해주세요."),
    UNAUTHORIZED(401, "인증 정보가 유효하지 않습니다."),
    USER_NOT_FOUND(404, "사용자 정보를 찾을 수 없습니다."),

    // 발주·장바구니
    ALREADY_PROCESSED_DETAIL(409, "이미 처리된 발주 상세 항목은 상태를 변경할 수 없습니다."),
    CART_ITEM_NOT_FOUND(404, "장바구니 항목을 찾을 수 없습니다."),
    EMPTY_ORDER(400, "발주할 품목이 없습니다."),
    EMPTY_ORDER_ITEMS(400, "처리할 품목이 없습니다."),
    EMPTY_SELECTION(400, "선택된 상품이 없습니다."),
    EXCEL_ERROR(500, "엑셀 파일 생성 중 오류가 발생했습니다."),
    INVALID_ITEM_LINE(400, "품목 ID와 수량(1 이상)이 필요합니다."),
    INVALID_ORDER_STATUS(400, "현재 상태에서는 처리할 수 없는 발주입니다."),
    NOT_PAID_ORDER(400, "결제가 완료되지 않은 발주 건입니다."),
    ORDER_DETAIL_NOT_FOUND(404, "해당 발주에 속한 상세 항목을 찾을 수 없습니다."),
    ORDER_NOT_FOUND(404, "발주 정보를 찾을 수 없습니다."),
    WAITING_ROLLBACK_FORBIDDEN(400, "대기 상태로 되돌릴 수 없습니다."),

    // 재고·상품
    ALREADY_DELETED(409, "이미 삭제된 상품입니다."),
    CATEGORY_NOT_FOUND(404, "카테고리 정보를 찾을 수 없습니다."),
    DUPLICATE_ITEM_NAME(409, "이미 등록된 상품명입니다."),
    FILE_EMPTY(400, "업로드할 파일이 없습니다."),
    ITEM_NOT_FOUND(404, "상품 정보를 찾을 수 없습니다."),
    STOCK_SHORTAGE(400, "재고가 부족합니다."),

    // 예치금·결제
    ACCOUNT_NOT_FOUND(404, "예치금 계좌를 찾을 수 없습니다."),
    DAILY_DEPOSIT_LIMIT_EXCEEDED(400, "1일 예치금 사용 한도를 초과했습니다."),
    INSUFFICIENT_BALANCE(400, "예치금 잔액이 부족합니다. 충전 후 다시 시도해 주세요."),
    INVALID_AMOUNT(400, "금액은 0보다 커야 합니다."),
    INVALID_DAILY_DEPOSIT_LIMIT(400, "1일 예치금 한도는 0 이상이어야 합니다."),
    OWNER_APPROVAL_REQUIRED(403, "직원 기안은 점주가 승인·결제한 뒤에만 본사로 전달됩니다. 예치금 결제는 점주 화면에서 진행해 주세요."),
    REQUEST_NOT_FOUND(404, "충전 요청을 찾을 수 없습니다."),

    // 배송
    DELIVERY_NOT_FOUND(404, "배송 정보를 찾을 수 없습니다."),
    INVALID_DELIVERY_STATUS(400, "현재 상태에서는 처리할 수 없는 배송입니다."),
    INVALID_TRACKING_NUMBER(400, "운송장 번호는 필수입니다."),
    ORDER_SHIPPING_SNAPSHOT_MISSING(400, "발주에 저장된 배송 정보가 없습니다."),
    WAYBILL_INTERRUPTED(500, "운송장 발급이 중단되었습니다."),

    // 문의
    COMMENT_NOT_FOUND(404, "댓글을 찾을 수 없습니다."),
    INVALID_COMMENT_DEPTH(400, "답댓글에는 답댓글을 달 수 없습니다."),

    // 연차
    INVALID_LEAVE_PERIOD(400, "시작일은 종료일보다 늦을 수 없습니다."),
    INVALID_LEAVE_STATUS(400, "허용되지 않은 처리 상태입니다."),
    LEAVE_NOT_FOUND(404, "연차 신청 정보를 찾을 수 없습니다."),
    LEAVE_PERIOD_OVERLAP(409, "이미 신청된 연차 기간과 겹칩니다."),
    PROCESSOR_NOT_FOUND(404, "처리자 정보를 찾을 수 없습니다."),
    REJECT_REASON_REQUIRED(400, "반려 사유를 입력해야 합니다."),

    // 파일
    FILE_DOWNLOAD_ERROR(500, "첨부파일 다운로드 중 오류가 발생했습니다."),
    FILE_NOT_FOUND(404, "첨부파일을 찾을 수 없습니다."),
    FILE_UPLOAD_ERROR(500, "파일 업로드 중 오류가 발생했습니다."),
    INVALID_FILE_EXTENSION(400, "허용되지 않는 파일 형식입니다."),
    INVALID_FILE_NAME(400, "파일 이름이 유효하지 않습니다."),
    INVALID_FILE_PATH(400, "잘못된 파일 경로입니다."),

    // AI 연동
    AI_API_ERROR(503, "AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    AI_DESERIALIZE_ERROR(500, "AI 추천 데이터 파싱에 실패했습니다."),
    AI_DRAFT_ALREADY_EXISTS(409, "이미 AI 초안이 생성된 문의입니다."),
    AI_EXECUTION_ERROR(500, "AI 분석 처리 중 내부 오류가 발생했습니다."),
    AI_INTERRUPTED(500, "AI 분석 요청이 중단되었습니다."),
    AI_INVALID_RESPONSE(500, "AI 응답 형식이 올바르지 않습니다. 잠시 후 다시 시도해 주세요."),
    AI_NOT_CONFIGURED(503, "AI 서비스가 설정되지 않아 사용할 수 없습니다. 관리자에게 문의해 주세요."),
    AI_PARSE_ERROR(500, "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    AI_SERVICE_ERROR(500, "AI 분석 중 오류가 발생했습니다."),
    AI_TIMEOUT(504, "AI 분석 요청이 시간 초과되었습니다. 잠시 후 다시 시도해 주세요."),
    INQUIRY_NOT_FOUND(404, "문의글을 찾을 수 없습니다."),
    SYSTEM_ADMIN_MISSING(500, "시스템 관리자 계정이 없어 자동 발주를 처리할 수 없습니다. 관리자에게 문의해 주세요.");

    private final int status;
    private final String message;
}
