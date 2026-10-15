package com.jstudy.inout.common.dto;

import org.springframework.http.ResponseEntity;

public class ResponseResult {

    public static ResponseEntity<?> fail(String message) {
        return fail(message, null);
    }

    public static ResponseEntity<?> fail(String message, Object data) {
        return ResponseEntity.badRequest().body(ResponseMessage.fail(message, data));
    }
    
    public static ResponseEntity<?> fail(String message, Object data, int status) {
        return ResponseEntity.status(status).body(ResponseMessage.fail(message, data, status));
    }

    public static ResponseEntity<?> fail(String message, int status) {
        return fail(message, null, status);
    }

    public static ResponseEntity<?> fail(String message, Object data, String resultCode, int status) {
        return ResponseEntity.status(status)
                .body(ResponseMessage.fail(message, data, resultCode, status));
    }

    public static ResponseEntity<?> fail(String message, String resultCode, int status) {
        return fail(message, null, resultCode, status);
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
