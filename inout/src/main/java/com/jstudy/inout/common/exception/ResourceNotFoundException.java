package com.jstudy.inout.common.exception;


public class ResourceNotFoundException extends InoutException {

    private static final int HTTP_STATUS = 404;

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + "(ID: " + id + ")을(를) 찾을 수 없습니다.", HTTP_STATUS);
    }
    
    public ResourceNotFoundException(String message) {
        super(message, HTTP_STATUS);
    }
}