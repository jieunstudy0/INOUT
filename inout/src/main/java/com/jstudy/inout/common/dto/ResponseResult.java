package com.jstudy.inout.common.dto;

import org.springframework.http.ResponseEntity;
import com.jstudy.inout.common.exception.ErrorCode;

public class ResponseResult {

    /*
     * 실패 응답은 ErrorCode로만 만든다. 상태 줄·header.status·resultCode가 모두 같은 ErrorCode에서 나오므로
     * 서로 어긋날 수 없다. 데이터를 담는 실패는 fail(ErrorCode, String)과 오버로딩하면 null 인자가
     * String 버전으로 선택되는 문제가 있어 failWithData로 이름을 나눴다.
     */
    public static ResponseEntity<?> fail(ErrorCode code) {
        return fail(code, code.getMessage());
    }

    public static ResponseEntity<?> fail(ErrorCode code, String message) {
        return build(code, message, null);
    }

    public static ResponseEntity<?> failWithData(ErrorCode code, Object data) {
        return build(code, code.getMessage(), data);
    }

    private static ResponseEntity<?> build(ErrorCode code, String message, Object data) {
        return ResponseEntity.status(code.getStatus())
                .body(ResponseMessage.fail(message, data, code.name(), code.getStatus()));
    }

    /*
     * 성공 응답의 message는 API 문서·로그용 설명이다. 화면에 표시할 성공 문구는 프론트엔드가 정하고,
     * 실패 문구만 header.message를 그대로 표시한다.
     */
    public static ResponseEntity<?> success() {
        return successWithData((Object) null);
    }

    public static ResponseEntity<?> successWithMessage(String message) {
        return ResponseEntity.ok().body(ResponseMessage.success(message, null));
    }

    public static ResponseEntity<?> successWithData(Object data) {
        return ResponseEntity.ok().body(ResponseMessage.success(data));
    }
    
    public static ResponseEntity<?> success(String message, Object data) {
        return ResponseEntity.ok().body(ResponseMessage.success(message, data));
    }
}
