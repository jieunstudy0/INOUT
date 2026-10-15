package com.jstudy.inout.common.exception;

public class UserNotFoundException extends InoutException {

    private static final int HTTP_STATUS = 404;

    public UserNotFoundException(String message) {
        super(message, HTTP_STATUS, "USER_NOT_FOUND");
    }
}
