package com.jstudy.inout.common.config.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * 검증 실패 시 사용자가 입력한 값(rejected value)이 로그와 응답 어디에도 남지 않는지 검사한다.
 */
class GlobalExceptionHandlerValidationLogTest {

    private static final String SECRET = "my-Secret-Pw-123";

    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    /** 컨트롤러 메서드 흉내: MethodParameter를 만들기 위한 대상 */
    @SuppressWarnings("unused")
    void signUp(SignUpForm form) {
    }

    static class SignUpForm {
        public String password;
    }

    private MethodArgumentNotValidException passwordValidationFailure() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SignUpForm(), "userInput");
        bindingResult.addError(new FieldError("userInput", "password", SECRET, false,
                new String[] {"Size.userInput.password", "Size"}, null, "비밀번호는 4자 이상 입력해야 합니다."));
        MethodParameter parameter = new MethodParameter(getClass().getDeclaredMethod("signUp", SignUpForm.class), 0);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @Test
    @DisplayName("검증 실패 로그에는 필드 이름과 규칙만 남고 입력값은 남지 않는다")
    void logDoesNotContainRejectedValue() throws Exception {
        MethodArgumentNotValidException exception = passwordValidationFailure();
        // 전제: 예외 메시지 자체에는 입력값이 들어 있다 (이 값을 그대로 로그에 쓰면 안 되는 이유)
        assertThat(exception.getMessage()).contains(SECRET);

        new GlobalExceptionHandler().handleMethodArgumentNotValidException(exception);

        List<String> logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0))
                .doesNotContain(SECRET)
                .contains("password(Size)");
    }

    @Test
    @DisplayName("검증 실패 응답에는 필드 이름과 문구만 담기고 입력값은 담기지 않는다")
    void responseDoesNotContainRejectedValue() throws Exception {
        ResponseEntity<?> response = new GlobalExceptionHandler()
                .handleMethodArgumentNotValidException(passwordValidationFailure());

        String json = new ObjectMapper().writeValueAsString(response.getBody());
        assertThat(json)
                .doesNotContain(SECRET)
                .contains("\"field\":\"password\"")
                .contains("VALIDATION_ERROR");
    }
}
