package com.jstudy.inout.common.dto;

import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseMessage {

    private ResponseMessageHeader header;
    private Object body;
    
    public static ResponseMessage fail(String message) {
        return fail(message, null);
    }

    public static ResponseMessage fail(String message, Object data) {
        return fail(message, data, "", HttpStatus.BAD_REQUEST.value());
    }

    public static ResponseMessage fail(String message, Object data, int status) {
        return fail(message, data, "", status);
    }

    public static ResponseMessage fail(String message, Object data, String resultCode, int status) {
        return ResponseMessage.builder()
    
                .header(ResponseMessageHeader.builder()
                        .result(false)
                        .resultCode(resultCode)
                        .message(message)
                        .status(status)
                        .build())
                .body(data)
                .build();
    }

    
    public static ResponseMessage success(Object data) {
        return success("", data);
    }
    
    public static ResponseMessage success(String message, Object data) {
        return ResponseMessage.builder()
                .header(ResponseMessageHeader.builder()
                        .result(true)
                        .resultCode("SUCCESS")
                        .message(message)
                        .status(HttpStatus.OK.value())
                        .build())
                .body(data)
                .build();
    }

    public static ResponseMessage success() {
        return success(null);
    }
}