package com.jstudy.inout.common.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.dto.ResponseMessage;
import com.jstudy.inout.common.exception.ErrorCode;
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
        
        response.setStatus(ErrorCode.AUTH_401.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); 
        response.setCharacterEncoding("UTF-8");

        ResponseMessage message = ResponseMessage.fail(ErrorCode.AUTH_401);

        String json = objectMapper.writeValueAsString(message);
        response.getWriter().write(json);
    }
}