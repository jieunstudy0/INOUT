package com.jstudy.inout.common.exception;

public class UserNotFoundException extends InoutException {

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}
