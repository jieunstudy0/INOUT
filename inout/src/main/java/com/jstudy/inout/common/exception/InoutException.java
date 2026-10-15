package com.jstudy.inout.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외. 상태 코드와 resultCode는 항상 {@link ErrorCode}가 정하고,
 * 호출하는 쪽은 상황에 맞는 문구만 바꿀 수 있다.
 */
@Getter
public class InoutException extends RuntimeException {

    private final ErrorCode code;

    public InoutException(ErrorCode code) {
        this(code, code.getMessage());
    }

    public InoutException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /** HTTP 상태 코드 */
    public int getStatus() {
        return code.getStatus();
    }

    /** 응답 header.resultCode로 나가는 값 */
    public String getResultCode() {
        return code.name();
    }
}
