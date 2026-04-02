package com.jstudy.inout.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
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

@Slf4j
@Component
public class JwtTokenProvider {
 
    private final Key key;
    private final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60; 
    
    public JwtTokenProvider(@Value("${jwt.secret:default-secret-key-must-be-long-enough-for-hs512}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtToken generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date issuedAt = new Date(now);
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        Date refreshTokenExpiresIn = new Date(now + 1000 * 60 * 60 * 24 * 7); 

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

        String rolesString = claims.get("roles", String.class);
        if (rolesString == null || rolesString.isEmpty()) {
             throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(rolesString.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
    }
    

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

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String generateAccessToken(com.jstudy.inout.common.auth.entity.User user) {

        String authorities = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .collect(Collectors.joining(","));

        if (authorities.isEmpty()) {
            authorities = "ROLE_USER"; 
        }

        long now = (new Date()).getTime();
        Date issuedAt = new Date(now);
        Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .claim(Claims.SUBJECT, user.getEmail()) 
                .claim(Claims.ISSUED_AT, issuedAt) 
                .claim(Claims.EXPIRATION, accessTokenExpiresIn) 
                .claim("roles", authorities) 
                .signWith(key, SignatureAlgorithm.HS256) 
                .compact();
    }
}