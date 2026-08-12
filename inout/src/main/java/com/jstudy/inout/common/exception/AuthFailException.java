package com.jstudy.inout.common.exception;


public class AuthFailException extends InoutException {

 private static final int HTTP_STATUS = 401; 

 public AuthFailException(String message) {
     super(message, HTTP_STATUS); 
 }
}
