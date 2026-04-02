package com.jstudy.inout.common.exception;

public class UnauthorizedException extends InoutException {

    private static final int HTTP_STATUS = 403;

    public UnauthorizedException(String message) {
        super(message, HTTP_STATUS);
    }
    
    public UnauthorizedException() {
        super("해당 작업을 수행할 권한이 없습니다.", HTTP_STATUS);
    }
}