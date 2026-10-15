package com.jstudy.inout.common.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jstudy.inout.common.dto.ResponseMessage;
import com.jstudy.inout.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

    
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(ErrorCode.AUTH_403.getStatus());

        ResponseMessage message = ResponseMessage.fail(ErrorCode.AUTH_403);

        response.getWriter().write(objectMapper.writeValueAsString(message));
    }
}