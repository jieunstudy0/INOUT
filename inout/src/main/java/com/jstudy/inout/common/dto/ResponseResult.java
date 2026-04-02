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
    
    public static ResponseEntity<?> success() {
        return success(null);
    }

    public static ResponseEntity<?> success(Object data) {
        return ResponseEntity.ok().body(ResponseMessage.success(data));
    }
    
    public static ResponseEntity<?> success(String message, Object data) {
        return ResponseEntity.ok().body(ResponseMessage.success(message, data));
    }
  
    public static ResponseEntity<?> result(ServiceResult result) {
        if (result.isFail()) {
            return fail(result.getMessage());
        }
        return success();
    }
}
