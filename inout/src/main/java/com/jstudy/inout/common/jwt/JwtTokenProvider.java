package com.jstudy.inout.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.jstudy.inout.common.jwt.dto.JwtToken;
import lombok.extern.slf4j.Slf4j;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT(Json Web Token) 생성 · 파싱 · 검증 담당 컴포넌트.
 *
 * [토큰 구조]
 * - AccessToken: 유효기간 1시간, 권한(roles) 클레임 포함
 * - RefreshToken: 유효기간 7일, subject(이메일)만 포함
 *
 * 서명 알고리즘: HS256 (HMAC-SHA256).
 * 시크릿 키는 application-secret.properties의 jwt.secret 에서 주입됩니다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** HMAC-SHA 서명에 사용되는 비밀 키 객체. */
    private final Key key;

    /** AccessToken 유효 시간: 1시간 (밀리초 단위). */
    private final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60;

    /**
     * 생성자 주입 시 Base64로 인코딩된 시크릿 키를 디코딩하여 Key 객체로 변환합니다.
     *
     * @param secretKey application-secret.properties의 jwt.secret 값
     */
    public JwtTokenProvider(
            @Value("${jwt.secret:default-secret-key-must-be-long-enough-for-hs512}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Spring Security Authentication 객체로부터 AccessToken + RefreshToken을 한 번에 생성합니다.
     * 로그인 성공 직후 AuthLoginController 에서 호출됩니다.
     *
     * @param authentication 인증 완료된 Authentication 객체
     * @return AccessToken + RefreshToken을 담은 JwtToken
     */
    public JwtToken generateToken(Authentication authentication) {
        // 권한 목록을 콤마로 이어 붙인 문자열로 변환 (예: "ROLE_ADMIN,ROLE_USER")
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date issuedAt = new Date(now);
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        Date refreshTokenExpiresIn = new Date(now + 1000L * 60 * 60 * 24 * 7); // 7일

        // AccessToken: subject(이메일) + roles 클레임 포함
        String accessToken = Jwts.builder()
                .claim(Claims.SUBJECT, authentication.getName())   // 이메일
                .claim(Claims.ISSUED_AT, issuedAt)                 // 발급 시각
                .claim(Claims.EXPIRATION, accessTokenExpiresIn)    // 만료 시각
                .claim("roles", authorities)                       // 권한 정보
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // RefreshToken: subject(이메일)만 포함 (최소 정보)
        String refreshToken = Jwts.builder()
                .claim(Claims.SUBJECT, authentication.getName())
                .claim(Claims.ISSUED_AT, issuedAt)
                .claim(Claims.EXPIRATION, refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * AccessToken에서 인증 정보(Authentication)를 추출합니다.
     * JwtAuthenticationFilter 에서 매 요청마다 호출됩니다.
     *
     * @param accessToken 클라이언트가 전송한 AccessToken
     * @return SecurityContext에 저장할 Authentication 객체
     * @throws RuntimeException roles 클레임이 없는 경우
     */
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        String rolesString = claims.get("roles", String.class);
        if (rolesString == null || rolesString.isEmpty()) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 콤마로 분리된 권한 문자열을 GrantedAuthority 컬렉션으로 변환
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(rolesString.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // principal은 이메일(subject), credentials는 null (JWT는 비밀번호 불필요)
        return new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
    }

    /**
     * 토큰의 서명·형식·만료 여부를 검증합니다.
     * 유효하면 true, 문제가 있으면 false를 반환합니다 (예외 미발생).
     *
     * @param token 검증할 JWT 문자열
     * @return 유효하면 true
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("JWT 서명/형식 오류");
        } catch (ExpiredJwtException e) {
            log.warn("JWT 만료됨");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 형식");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 클레임 없음");
        }
        return false;
    }

    /**
     * 토큰에서 Claims를 추출합니다.
     * 만료된 토큰이더라도 Claims는 추출 가능 (갱신 흐름에서 사용).
     *
     * @param accessToken JWT 문자열
     * @return Claims 객체
     */
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims는 반환 (RefreshToken 재발급 흐름)
            return e.getClaims();
        }
    }

    /**
     * User 엔티티로부터 직접 AccessToken을 생성합니다.
     * RefreshToken 갱신 요청(/api/user/refresh)에서 호출됩니다.
     * Authentication 없이 User 엔티티만으로 토큰을 만들어야 할 때 사용합니다.
     *
     * @param user 토큰 발급 대상 사용자
     * @return 새로 발급된 AccessToken 문자열
     */
    public String generateAccessToken(com.jstudy.inout.common.auth.entity.User user) {
        // UserRole에서 권한 이름 추출
        String authorities = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .collect(Collectors.joining(","));

        // 권한이 없는 경우 기본값 부여 (비정상 상황 방어)
        if (authorities.isEmpty()) {
            authorities = "ROLE_USER";
        }

        long now = (new Date()).getTime();

        return Jwts.builder()
                .claim(Claims.SUBJECT, user.getEmail())
                .claim(Claims.ISSUED_AT, new Date(now))
                .claim(Claims.EXPIRATION, new Date(now + ACCESS_TOKEN_EXPIRE_TIME))
                .claim("roles", authorities)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}