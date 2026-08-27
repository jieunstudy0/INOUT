package com.jstudy.inout.common.oauth2;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.entity.RefreshToken;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.RefreshTokenRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.jwt.JwtTokenProvider;
import com.jstudy.inout.common.jwt.dto.JwtToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final Duration REFRESH_TOKEN_COOKIE_MAX_AGE = Duration.ofDays(7);

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:5173/oauth2/callback}")
    private String authorizedRedirectUri;

    /** GUEST 사용자를 온보딩 페이지로 리다이렉트하는 URI (프론트엔드 경로) */
    @Value("${app.oauth2.onboarding-redirect-uri:http://localhost:5173/onboarding/complete-profile}")
    private String onboardingRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2UserDetails oAuth2Principal = (CustomOAuth2UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(oAuth2Principal.getUser().getEmail())
                .orElseThrow(() -> new IllegalStateException(
                        "OAuth2 성공 핸들러: 사용자를 찾을 수 없습니다. email=" + oAuth2Principal.getUser().getEmail()));
      
        List<SimpleGrantedAuthority> authorities = user.getUserRoles().stream()
                .map(ur -> {
                    String name = ur.getRole().getRoleName();
                    return new SimpleGrantedAuthority(name.startsWith("ROLE_") ? name : "ROLE_" + name);
                })
                .collect(Collectors.toList());

        Authentication jwtAuthentication = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(user), null, authorities);

        JwtToken jwtToken = jwtTokenProvider.generateToken(jwtAuthentication);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(jwtToken.getRefreshToken())
                .expiresAt(LocalDateTime.now().plus(REFRESH_TOKEN_COOKIE_MAX_AGE))
                .build());

        String role = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_EMPLOYEE");

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, jwtToken.getRefreshToken())
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // ROLE_GUEST(소셜 신규 가입) → 온보딩 페이지로 분기
        boolean isGuest = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_GUEST"::equals);

        String baseUri = isGuest ? onboardingRedirectUri : authorizedRedirectUri;
        String targetUrl = UriComponentsBuilder.fromUriString(baseUri)
                .queryParam("accessToken", jwtToken.getAccessToken())
                .queryParam("role", role)
                .build().toUriString();

        log.info("OAuth2 로그인 성공 — email={}, provider={}, role={}, onboarding={}",
                user.getEmail(), user.getProvider(), role, isGuest);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
