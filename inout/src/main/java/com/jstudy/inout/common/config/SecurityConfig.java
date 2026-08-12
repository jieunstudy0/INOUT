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
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
