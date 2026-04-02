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
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
            .requestMatchers("/.well-known/**"); 
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
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
              
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                
                .authorizeHttpRequests((auth) -> auth
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .requestMatchers("/.well-known/**").permitAll()         
                    .requestMatchers(
                        "/api/user/login", 
                        "/user/register", 
                        "/user/login", 
                        "/user/find", 
                        "/user/resetPassword",
                        "/user/public/**" 
                    ).permitAll()

                    .requestMatchers("/admin/**", "/api/admin/**", "/stock/adm/**").hasRole("ADMIN")
                    .requestMatchers("/stock/emp/**", "/order/emp/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/inquiry/**").hasAnyRole("USER", "ADMIN")
                    .anyRequest().authenticated() 
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint) 
                        .accessDeniedHandler(accessDeniedHandler)
                );
        
        return http.build();
    }
}
