package com.jstudy.inout.common.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.dto.ResponseMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper(); 

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); 
        response.setCharacterEncoding("UTF-8");

        ResponseMessage message = ResponseMessage.fail(
            "인증 정보가 유효하지 않습니다. 로그인이 필요합니다.", 
            null, 
            "AUTH_401", 
            401
        );

        String json = objectMapper.writeValueAsString(message);
        response.getWriter().write(json);
    }
}