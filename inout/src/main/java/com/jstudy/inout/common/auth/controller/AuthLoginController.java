package com.jstudy.inout.common.auth.controller;

import jakarta.servlet.http.HttpServletResponse; 
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.HashMap;
import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.dto.UserLogin;
import com.jstudy.inout.common.auth.entity.RefreshToken;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.RefreshTokenRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.jwt.JwtTokenProvider;
import com.jstudy.inout.common.jwt.dto.JwtToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jstudy.inout.common.exception.InoutException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthLoginController {
    
	private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    
    @PostMapping("/api/user/login") 
    public ResponseEntity<?> login(@RequestBody @Valid UserLogin userLogin) {
        log.info("1.로그인 API 진입: {}", userLogin.getEmail()); 
        
        try {   
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLogin.getEmail(), userLogin.getPassword())
            );
            
            log.info("2.인증 성공!");
            JwtToken token = jwtTokenProvider.generateToken(authentication);

            User user = userRepository.findByEmail(userLogin.getEmail())
                    .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404));
            
 
            RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                    .map(rt -> {
                        rt.updateToken(token.getRefreshToken(), LocalDateTime.now().plusDays(7)); 
                        return rt;
                    })
                    .orElseGet(() -> RefreshToken.builder()
                            .user(user)
                            .token(token.getRefreshToken())
                            .expiresAt(LocalDateTime.now().plusDays(7)) 
                            .build()
                    );
            
            refreshTokenRepository.save(refreshToken); 
         
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER");

            log.info("[디버그] 최종 추출된 권한명: '{}'", role); 

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", token.getAccessToken());         
            responseData.put("refreshToken", token.getRefreshToken()); 
            responseData.put("role", role);

            return ResponseResult.success("로그인 성공", responseData);

        } catch (Exception e) {
            log.error("[에러] 로그인 인증 실패: {}", e.getMessage());
                      
            userRepository.findByEmail(userLogin.getEmail()).ifPresent(user -> {
                user.increaseLoginFailCount();
                userRepository.save(user);
                
                if(user.isLocked()) {
                    log.warn("🚨 계정이 잠겼습니다: {}", user.getEmail());
                }
            });

            return ResponseEntity.status(401).body(Map.of("message", "이메일 또는 비밀번호가 잘못되었습니다. (5회 실패 시 계정 잠금)"));
        }
    }
    
    @PostMapping("/api/user/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InoutException("유효하지 않은 토큰입니다.", 401));

        if (savedToken.isExpired()) {
            throw new InoutException("만료된 토큰입니다. 다시 로그인해주세요.", 401);
        }
 
        String newAccessToken = jwtTokenProvider.generateAccessToken(savedToken.getUser());

        return ResponseResult.success("토큰이 갱신되었습니다.", 
            Map.of("accessToken", newAccessToken));
    }
    
    @PostMapping("/api/user/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletResponse response) { 

        
        refreshTokenRepository.deleteByUser_Id(principal.getUser().getId());

       
        Cookie cookie = new Cookie("accessToken", null);
        cookie.setMaxAge(0); 
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseResult.success("로그아웃 되었습니다.");
    }
}
