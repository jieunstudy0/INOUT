package com.jstudy.inout.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ResponseError {

    private String field;
    private String message;

    public static ResponseError of(FieldError e) {
        return ResponseError.builder()
                .field(e.getField())
                .message(e.getDefaultMessage())
                .build();
    }

    public static List<ResponseError> of(List<ObjectError> errors) {
        List<ResponseError> responseErrors = new ArrayList<>();
        if (errors != null) {
            errors.forEach((e) -> {
                // 클래스 레벨 제약·reject()로 생긴 ObjectError는 FieldError가 아니므로 객체 이름을 field로 쓴다.
                if (e instanceof FieldError fieldError) {
                    responseErrors.add(ResponseError.of(fieldError));
                } else {
                    responseErrors.add(ResponseError.builder()
                            .field(e.getObjectName())
                            .message(e.getDefaultMessage())
                            .build());
                }
            });
        }
        return responseErrors;
    }

}
