package com.jstudy.inout.common.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 처리.
 * (예: CustomOAuth2UserService에서 이메일 미제공으로 OAuth2AuthenticationException을 던진 경우,
 *  redirect_uri_mismatch, 사용자가 동의 화면에서 취소한 경우 등)
 *
 * 실패 시 Spring Security 기본 동작(백엔드의 /login?error 페이지로 리다이렉트)은 SPA 프론트엔드와
 * 맞지 않으므로, 성공 핸들러와 동일한 프론트엔드 오리진의 로그인 페이지로 에러 메시지와 함께
 * 리다이렉트한다.
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:5173/oauth2/callback}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {

        String message = exception.getMessage() != null
                ? exception.getMessage()
                : "소셜 로그인에 실패했습니다. 다시 시도해 주세요.";

        log.warn("OAuth2 로그인 실패: {}", message);

        String targetUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .replacePath("/login")
                .replaceQuery(null)
                .queryParam("error", "oauth2")
                .queryParam("message", message)
                .build()
                .encode()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
