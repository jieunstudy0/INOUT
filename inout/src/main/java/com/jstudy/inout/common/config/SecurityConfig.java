package com.jstudy.inout.common.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.jstudy.inout.common.auth.filter.JwtAuthenticationFilter;
import com.jstudy.inout.common.config.handler.CustomAccessDeniedHandler;
import com.jstudy.inout.common.config.handler.CustomAuthenticationEntryPoint;
import com.jstudy.inout.common.jwt.JwtTokenProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security 핵심 설정 클래스.
 *
 * [인증/인가 흐름]
 * 1. 모든 요청은 JwtAuthenticationFilter 를 통과
 * 2. Authorization 헤더 또는 쿠키에서 JWT 추출 및 검증
 * 3. 유효한 토큰이면 SecurityContext에 Authentication 저장
 * 4. URL 패턴별 권한 규칙 적용 (아래 filterChain 참고)
 *
 * 세션 전략: STATELESS — JWT 기반이므로 서버에 세션을 저장하지 않습니다.
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 어노테이션 활성화
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 비밀번호 암호화 Bean.
     * BCrypt 알고리즘 사용 — 자동으로 솔트(salt) 생성 및 포함.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 자체를 건너뛸 경로 설정.
     * /.well-known/** 경로는 보안 검사 없이 완전히 통과시킵니다.
     * (브라우저 보안 정책, Apple 앱 연결 등에 사용)
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/.well-known/**");
    }

    /**
     * AuthenticationManager Bean.
     * AuthLoginController에서 로그인 처리 시 직접 주입하여 사용합니다.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 보안 필터 체인 설정.
     *
     * https://blog.naver.com/PostView.naver?blogId=dicstudy&logNo=221215714931&redirect=Dlog&widgetTypeCall=true&noTrackingCode=true
     * - 정적 리소스 (CSS, JS, 이미지): 모두 허용
     * - 로그인, 회원가입, 비밀번호 찾기 등 공개 API: 모두 허용
     * - /admin/**, /api/admin/**, /stock/adm/**: ADMIN 권한 필요
     * - /stock/emp/**, /order/emp/**: USER 또는 ADMIN 권한 필요
     * - /inquiry/**: USER 또는 ADMIN 권한 필요
     * - 나머지 모든 요청: 인증 필요
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 — JWT 방식에서는 CSRF 토큰이 불필요
                .csrf(csrf -> csrf.disable())
                // X-Frame-Options: SAMEORIGIN — 같은 도메인 iframe 허용 (Thymeleaf 레이아웃용)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // 폼 로그인 및 HTTP Basic 인증 비활성화 — JWT 방식 사용
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                // 세션 생성 안 함 — Stateless JWT 방식
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // UsernamePasswordAuthenticationFilter 앞에 JWT 필터 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests((auth) -> auth
                        // 1. 정적 리소스 허용
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/.well-known/**").permitAll()

                        // 2. 인증 불필요 API (공개 엔드포인트)
                        .requestMatchers(
                                "/api/user/login",
                                "/api/user/refresh", 
                                "/user/register",
                                "/user/login",
                                "/user/find",
                                "/user/resetPassword",
                                "/user/public/**"  // 이메일 확인, 비밀번호 초기화 등
                        ).permitAll()

                        // 3. 권한별 접근 제한
                        .requestMatchers("/admin/**", "/api/admin/**", "/stock/adm/**").hasRole("ADMIN")
                        .requestMatchers("/stock/emp/**", "/order/emp/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        .requestMatchers("/inquiry/**").hasAnyRole("EMPLOYEE", "ADMIN")

                        // 4. 나머지 모든 요청: 로그인 필요
                        .anyRequest().authenticated()
                )
                // 인증/인가 실패 시 커스텀 핸들러로 JSON 응답 반환
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)  // 401
                        .accessDeniedHandler(accessDeniedHandler)             // 403
                );

        return http.build();
    }
}