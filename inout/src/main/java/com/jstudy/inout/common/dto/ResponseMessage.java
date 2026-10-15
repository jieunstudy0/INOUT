package com.jstudy.inout.common.dto;

import org.springframework.http.HttpStatus;
import com.jstudy.inout.common.exception.ErrorCode;
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
    
    /** ResponseEntity를 쓸 수 없는 Security 필터 단계(401·403)에서 본문만 만들 때 사용한다. */
    public static ResponseMessage fail(ErrorCode code) {
        return fail(code.getMessage(), null, code.name(), code.getStatus());
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