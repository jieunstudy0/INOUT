package com.jstudy.inout.common.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.jstudy.inout.common.auth.filter.JwtAuthenticationFilter;
import com.jstudy.inout.common.config.handler.CustomAccessDeniedHandler;
import com.jstudy.inout.common.config.handler.CustomAuthenticationEntryPoint;
import com.jstudy.inout.common.jwt.JwtTokenProvider;
import com.jstudy.inout.common.oauth2.CustomOAuth2UserService;
import com.jstudy.inout.common.oauth2.OAuth2AuthenticationFailureHandler;
import com.jstudy.inout.common.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/.well-known/**", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/user/login",
                                "/api/user/refresh",
                                "/api/user/register",
                                "/api/user/public/**",
                                "/api/user/find",
                                "/api/user/resetPassword",
                                "/user/login"
                        ).permitAll()
                        // ROLE_GUEST 전용: 소셜 온보딩 완료 API만 접근 허용.
                        // 나머지 업무 API(/api/emp/**, /api/owner/** 등)는 아래 역할 규칙에서 차단된다.
                        .requestMatchers("/api/auth/social/complete-profile").hasRole("GUEST")
                        // Spring Security OAuth2 Client의 기본 진입점(인가 요청)과 콜백 엔드포인트.
                        // JWT가 없는 최초 요청이므로 반드시 permitAll 이어야 하며, 그렇지 않으면
                        // JwtAuthenticationFilter 통과 후 anyRequest().authenticated()에 걸려
                        // CustomAuthenticationEntryPoint가 401(AUTH_401)을 즉시 반환한다.
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        .requestMatchers("/admin/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/owner/**").hasRole("OWNER")
                        .requestMatchers(
                                "/api/emp/**",
                                "/stock/emp/**",
                                "/order/emp/**"
                        ).hasAnyRole("EMPLOYEE", "ADMIN")
                        .requestMatchers("/api/inquiry/**", "/inquiry/**").hasAnyRole("EMPLOYEE", "OWNER", "ADMIN")
                        // 예치금 즉시 충전·환불은 ADMIN만 (점주 충전은 /api/owner/charges)
                        .requestMatchers(HttpMethod.POST, "/api/deposit/**").hasRole("ADMIN")
                        .requestMatchers("/api/payment/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        .requestMatchers("/api/emp/deposit").hasAnyRole("EMPLOYEE", "ADMIN")
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/deliveries/**").authenticated()
                        .anyRequest().authenticated()
                )
                // 카카오/네이버/구글 소셜 로그인 — 이전에는 CustomOAuth2UserService /
                // OAuth2AuthenticationSuccessHandler 구현체만 존재하고 실제로 필터체인에
                // 등록되어 있지 않아 "/oauth2/authorization/{provider}" 요청이 그대로
                // anyRequest().authenticated() 규칙에 걸려 401(AUTH_401)로 막혔었다.
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
