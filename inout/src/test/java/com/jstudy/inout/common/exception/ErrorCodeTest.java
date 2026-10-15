package com.jstudy.inout.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import com.jstudy.inout.common.config.handler.GlobalExceptionHandler;
import com.jstudy.inout.common.dto.ResponseMessage;

/**
 * 실패 문구 작성 규칙(docs/study/ERROR_MESSAGE_GUIDE.md)을 ErrorCode에 강제한다.
 */
class ErrorCodeTest {

    /** 사용자에게 보이면 안 되는 내부 정보(설정 파일, DB, 외부 API 이름, enum 값 등) */
    private static final List<String> INTERNAL_TOKENS = List.of(
            "application-", "properties", "DB", "Gemini", "candidates", "JSON", "AUTO_INCREMENT", "ROLE_", "id=");

    @Test
    @DisplayName("모든 ErrorCode는 4xx·5xx 상태를 가진다")
    void statusIsClientOrServerError() {
        assertThat(ErrorCode.values())
                .allSatisfy(code -> assertThat(code.getStatus()).as(code.name()).isBetween(400, 599));
    }

    @Test
    @DisplayName("모든 기본 문구는 마침표로 끝나는 완결된 문장이다")
    void messageIsCompleteSentence() {
        assertThat(ErrorCode.values())
                .allSatisfy(code -> assertThat(code.getMessage()).as(code.name()).isNotBlank().endsWith("."));
    }

    @Test
    @DisplayName("기본 문구에 내부 정보가 들어가지 않는다")
    void messageHasNoInternalDetail() {
        assertThat(ErrorCode.values()).allSatisfy(code ->
                assertThat(INTERNAL_TOKENS).as(code.name())
                        .noneMatch(token -> code.getMessage().contains(token)));
    }

    @Test
    @DisplayName("InoutException의 상태·resultCode는 ErrorCode가 정하고 문구만 바꿀 수 있다")
    void exceptionTakesStatusAndCodeFromErrorCode() {
        InoutException e = new InoutException(ErrorCode.NOT_OWNER, "본인이 작성한 댓글만 삭제할 수 있습니다.");

        assertThat(e.getStatus()).isEqualTo(403);
        assertThat(e.getResultCode()).isEqualTo("NOT_OWNER");
        assertThat(e.getMessage()).isEqualTo("본인이 작성한 댓글만 삭제할 수 있습니다.");
        assertThat(new InoutException(ErrorCode.USER_NOT_FOUND).getMessage())
                .isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 응답은 resultCode LOGIN_FAILED와 401을 가진다 (이전에는 오버로딩 선택 문제로 null)")
    void loginFailureHasResultCode() {
        ResponseEntity<?> response = new GlobalExceptionHandler()
                .handleBadCredentialsException(new BadCredentialsException("bad"));
        ResponseMessage body = (ResponseMessage) response.getBody();

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(body.getHeader().getResultCode()).isEqualTo("LOGIN_FAILED");
        assertThat(body.getHeader().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("상태 줄과 header.status는 항상 같은 ErrorCode에서 나와 일치한다")
    void statusLineMatchesHeaderStatus() {
        Arrays.stream(ErrorCode.values()).forEach(code -> {
            ResponseEntity<?> response = com.jstudy.inout.common.dto.ResponseResult.fail(code);
            ResponseMessage body = (ResponseMessage) response.getBody();
            assertThat(response.getStatusCode().value()).as(code.name()).isEqualTo(body.getHeader().getStatus());
            assertThat(body.getHeader().getResultCode()).isEqualTo(code.name());
        });
    }
}
