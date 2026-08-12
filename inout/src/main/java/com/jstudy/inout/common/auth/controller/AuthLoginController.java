package com.jstudy.inout.common.auth.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
import com.jstudy.inout.common.auth.service.AuthService;
import com.jstudy.inout.common.dto.ResponseResult;
import com.jstudy.inout.common.jwt.JwtTokenProvider;
import com.jstudy.inout.common.jwt.dto.JwtToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jstudy.inout.common.exception.InoutException;

@Tag(name = "인증 (Auth)", description = "로그인, 토큰 갱신, 로그아웃. 로그인·갱신 API는 인증 없이 호출 가능합니다.")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthLoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Operation(summary = "로그인",
               description = "이메일·비밀번호로 인증하여 accessToken(1시간)과 refreshToken(7일)을 발급합니다. 5회 실패 시 계정이 잠깁니다.")
    @SecurityRequirements 
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 — accessToken, refreshToken, role 반환"),
            @ApiResponse(responseCode = "401", description = "이메일/비밀번호 오류 또는 5회 실패 잠금"),
            @ApiResponse(responseCode = "403", description = "계정 잠금 상태")
    })
    @PostMapping("/api/user/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserLogin userLogin) {
        log.info("1.로그인 API 진입: {}", userLogin.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLogin.getEmail(), userLogin.getPassword())
            );

            log.info("2.인증 성공!");

            authService.loginSuccess(userLogin.getEmail());

            JwtToken token = jwtTokenProvider.generateToken(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

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
                    .orElse("ROLE_EMPLOYEE");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", token.getAccessToken());
            responseData.put("refreshToken", token.getRefreshToken());
            responseData.put("role", role);

            return ResponseResult.success("로그인 성공", responseData);

        } catch (LockedException e) {
            throw new InoutException("계정이 잠겼습니다. 관리자에게 문의해주세요.", 403, "ACCOUNT_LOCKED");

        } catch (DisabledException e) {
            throw new InoutException(
                    e.getMessage() != null ? e.getMessage() : "비활성 계정입니다. 로그인이 불가합니다.",
                    403,
                    "ACCOUNT_DISABLED");

        } catch (Exception e) {
            log.warn("로그인 실패: {}", userLogin.getEmail());

            authService.loginFailed(userLogin.getEmail());

            throw new InoutException("이메일 또는 비밀번호가 잘못되었거나, 5회 이상 실패하여 계정이 잠겼습니다.", 401, "UNAUTHORIZED");
        }
    }
    
    
    @Operation(summary = "Access Token 갱신",
               description = "refreshToken으로 새 accessToken을 발급합니다. refreshToken은 갱신되지 않습니다(7일 유효).")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "새 accessToken 반환"),
            @ApiResponse(responseCode = "400", description = "refreshToken 누락"),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 refreshToken")
    })
    @PostMapping("/api/user/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (!StringUtils.hasText(refreshToken)) {
            throw new InoutException("리프레시 토큰이 필요합니다.", 400, "BAD_REQUEST");
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InoutException("유효하지 않거나 만료된 리프레시 토큰입니다.", 401, "UNAUTHORIZED");
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InoutException("유효하지 않은 토큰입니다.", 401, "UNAUTHORIZED"));

        if (savedToken.isExpired()) {
            throw new InoutException("만료된 토큰입니다. 다시 로그인해주세요.", 401, "TOKEN_EXPIRED");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(savedToken.getUser());

        return ResponseResult.success("토큰이 갱신되었습니다.",
                Map.of("accessToken", newAccessToken));
    }

    @Operation(summary = "로그아웃", description = "DB에서 refreshToken을 삭제하고 accessToken 쿠키를 만료시킵니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/api/user/logout")
    @Transactional
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletResponse response) {

        refreshTokenRepository.deleteByUser_Id(principal.getUser().getId());

        Cookie cookie = new Cookie("accessToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseResult.success("로그아웃 되었습니다.", null);
    }
}