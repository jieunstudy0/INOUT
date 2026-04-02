package com.jstudy.inout.common.config.handler;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.jstudy.inout.common.dto.ResponseError;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.exception.UserNotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(InoutException.class)
	public ResponseEntity<?> handleInoutException(InoutException exception) {
	    return ResponseResult.fail(
	        exception.getMessage(), 
	        null, 
	        exception.getResultCode(),
	        exception.getErrorCode()   
	    );
	}
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException e) {
	    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.warn("Validation Error occurred: {}", exception.getMessage());
        List<ResponseError> errors = ResponseError.of(exception.getBindingResult().getAllErrors());
        return ResponseResult.fail("입력 데이터에 유효성 오류가 있습니다.", errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException exception) {
        return ResponseResult.fail("아이디 또는 비밀번호가 일치하지 않습니다.", null, 401);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception exception) {
        log.error("Unhandled Exception occurred: ", exception);
        return ResponseResult.fail("서버 내부에서 예상치 못한 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFoundException(UserNotFoundException exception) {
        log.warn("User Not Found: {}", exception.getMessage());
        return ResponseResult.fail(exception.getMessage(), null, 400);
    }
    
    @ExceptionHandler(AuthenticationException.class) 
    public ResponseEntity<?> handleAuthException(AuthenticationException e) {
        return ResponseResult.fail(
            "인증 정보가 유효하지 않습니다. 로그인이 필요합니다.", 
            null, 
            "AUTH_401", 
            401
        );   
}
    
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException exception) {
        log.warn("Concurrency Conflict (Optimistic Lock): {}", exception.getMessage());
        return ResponseResult.fail(
            "이미 다른 요청에 의해 처리되었습니다. 새로고침 후 다시 시도해주세요.", 
            null, 
            "CONCURRENCY_ERROR", 
            409                
        );
    }  
}
