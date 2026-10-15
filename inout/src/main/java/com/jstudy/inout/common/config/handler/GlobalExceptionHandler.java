package com.jstudy.inout.common.config.handler;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.jstudy.inout.common.dto.ResponseError;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InoutException.class)
    public ResponseEntity<?> handleInoutException(InoutException exception) {
        return ResponseResult.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException exception) {
        return ResponseResult.fail(ErrorCode.LOGIN_FAILED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthException(AuthenticationException e) {
        return ResponseResult.fail(ErrorCode.AUTH_401);
    }

    /**
     * 메서드 보안(@PreAuthorize) 거부는 DispatcherServlet 안에서 발생해 필터의 CustomAccessDeniedHandler까지
     * 가지 않는다. 처리하지 않으면 아래 Exception 핸들러가 500으로 응답하므로 URL 규칙과 같은 403으로 맞춘다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseResult.fail(ErrorCode.AUTH_403);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> handleOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException exception) {
        log.warn("Concurrency Conflict (Optimistic Lock): {}", exception.getMessage());
        return ResponseResult.fail(ErrorCode.CONCURRENCY_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        log.warn("Validation Error occurred: {}", exception.getMessage());
        List<ResponseError> errors = ResponseError.of(exception.getBindingResult().getAllErrors());
        return ResponseResult.failWithData(ErrorCode.VALIDATION_ERROR, errors);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception exception) {
        log.error("Unhandled Exception occurred: ", exception);
        return ResponseResult.fail(ErrorCode.INTERNAL_ERROR);
    }
}