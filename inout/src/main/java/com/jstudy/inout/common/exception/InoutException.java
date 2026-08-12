package com.jstudy.inout.common.exception;

import lombok.Getter;

@Getter 
public class InoutException extends RuntimeException { 

    private final int errorCode;    
    private final String resultCode;  

    public InoutException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.resultCode = ""; 
    }

    public InoutException(String message, int errorCode, String resultCode) {
        super(message);
        this.errorCode = errorCode;
        this.resultCode = resultCode;
    }

    public InoutException(String message) {
        super(message);
        this.errorCode = 400; 
        this.resultCode = "";
    }
}