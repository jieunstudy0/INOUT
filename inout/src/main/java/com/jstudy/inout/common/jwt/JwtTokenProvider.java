package com.jstudy.inout.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.jstudy.inout.common.auth.service.CustomUserDetailsService;
import com.jstudy.inout.common.jwt.dto.JwtToken;
import lombok.extern.slf4j.Slf4j;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secretKey,
            CustomUserDetailsService customUserDetailsService) {
        this.key = buildSigningKey(secretKey);
        this.customUserDetailsService = customUserDetailsService;
    }

    private static Key buildSigningKey(String secretKey) {
        String s = secretKey == null ? "" : secretKey.trim();
        if (s.isEmpty()) {
            throw new IllegalStateException(
                    "jwt.secret is not configured. Set a Base64-encoded key (>=32 bytes), "
                            + "or a hex string (>=64 hex chars = 32 bytes), in application-secret.properties.");
        }
        try {
            byte[] decoded = Decoders.BASE64.decode(s);
            if (decoded.length >= 32) {
                return Keys.hmacShaKeyFor(decoded);
            }
        } catch (IllegalArgumentException e) {
            log.debug("jwt.secret is not valid Base64, trying hex / UTF-8 fallbacks");
        }
        if (s.matches("[0-9a-fA-F]+") && s.length() % 2 == 0 && s.length() >= 64) {
            return Keys.hmacShaKeyFor(HexFormat.of().parseHex(s));
        }
        byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
        if (utf8.length >= 32) {
            return Keys.hmacShaKeyFor(utf8);
        }
        try {
            return Keys.hmacShaKeyFor(MessageDigest.getInstance("SHA-256").digest(utf8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public JwtToken generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = new Date().getTime();
        Date issuedAt = new Date(now);
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        Date refreshTokenExpiresIn = new Date(now + 1000L * 60 * 60 * 24 * 7);

        String accessToken = Jwts.builder()
                .claim(Claims.SUBJECT, authentication.getName())
                .claim(Claims.ISSUED_AT, issuedAt)
                .claim(Claims.EXPIRATION, accessTokenExpiresIn)
                .claim("roles", authorities)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

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

    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (claims.get("roles") == null) {
            throw new JwtException("권한 정보가 없는 토큰입니다.");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(claims.getSubject());

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.debug("JWT 서명/형식 오류: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료됨: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("지원되지 않는 JWT 형식: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT 클레임 없음: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(accessToken).getBody();
    }

    public String generateAccessToken(com.jstudy.inout.common.auth.entity.User user) {
        String authorities = user.getUserRoles().stream()
                .map(userRole -> {
                    String name = userRole.getRole().getRoleName();
                    return name.startsWith("ROLE_") ? name : "ROLE_" + name;
                })
                .collect(Collectors.joining(","));

        if (authorities.isEmpty()) {
            authorities = "ROLE_USER";
        }

        long now = new Date().getTime();
        return Jwts.builder()
                .claim(Claims.SUBJECT, user.getEmail())
                .claim(Claims.ISSUED_AT, new Date(now))
                .claim(Claims.EXPIRATION, new Date(now + ACCESS_TOKEN_EXPIRE_TIME))
                .claim("roles", authorities)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
