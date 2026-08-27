package com.jstudy.inout.common.auth.service;

import com.jstudy.inout.common.auth.dto.CustomUserDetails;
import com.jstudy.inout.common.auth.dto.SocialOnboardingRequest;
import com.jstudy.inout.common.auth.entity.RefreshToken;
import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import com.jstudy.inout.common.auth.repository.RefreshTokenRepository;
import com.jstudy.inout.common.auth.repository.RoleRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.repository.UserRoleRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.jwt.JwtTokenProvider;
import com.jstudy.inout.common.jwt.dto.JwtToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialOnboardingService {

    private static final Duration REFRESH_TOKEN_VALID = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 소셜 온보딩 완료:
     * 1. ROLE_GUEST 검증 (한 번만 허용)
     * 2. phone, birthday 확정
     * 3. ROLE_GUEST → 선택한 역할로 교체
     * 4. 정식 JWT 재발급
     */
    @Transactional
    public Map<String, String> complete(CustomUserDetails principal, SocialOnboardingRequest req) {
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new InoutException("사용자를 찾을 수 없습니다.", 404, "NOT_FOUND"));

        boolean isGuest = user.getUserRoles().stream()
                .anyMatch(ur -> "ROLE_GUEST".equals(ur.getRole().getRoleName()));
        if (!isGuest) {
            throw new InoutException("이미 온보딩이 완료된 계정입니다.", 400, "ALREADY_COMPLETED");
        }

        // 프로필 확정 (실명·전화·생년월일)
        user.completeSocialOnboarding(req.name(), req.phone(), req.birthday());

        // 역할 교체: ROLE_GUEST 삭제 후 선택 역할 부여
        String targetRoleName = "ROLE_" + req.role();
        userRoleRepository.deleteByUser(user);

        Role newRole = roleRepository.findByRoleName(targetRoleName)
                .orElseThrow(() -> new InoutException(
                        targetRoleName + " 역할이 존재하지 않습니다.", 500, "ROLE_NOT_FOUND"));

        userRoleRepository.save(UserRole.builder().user(user).role(newRole).build());
        // 변경 내용이 아래 JWT 발급 전에 flush되도록 영속성 컨텍스트 반영
        userRepository.flush();

        // 정식 JWT 발급
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(user),
                null,
                List.of(new SimpleGrantedAuthority(targetRoleName))
        );
        JwtToken token = jwtTokenProvider.generateToken(auth);

        // Refresh Token 저장(갱신)
        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        rt -> rt.updateToken(token.getRefreshToken(),
                                LocalDateTime.now().plus(REFRESH_TOKEN_VALID)),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .user(user)
                                .token(token.getRefreshToken())
                                .expiresAt(LocalDateTime.now().plus(REFRESH_TOKEN_VALID))
                                .build())
                );

        log.info("소셜 온보딩 완료: email={}, role={}", user.getEmail(), targetRoleName);
        return Map.of(
                "accessToken", token.getAccessToken(),
                "refreshToken", token.getRefreshToken(),
                "role", targetRoleName
        );
    }
}
