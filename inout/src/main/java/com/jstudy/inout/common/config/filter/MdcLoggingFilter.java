package com.jstudy.inout.common.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;


@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY         = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String incomingTraceId = httpRequest.getHeader(TRACE_ID_HEADER);
        String traceId = StringUtils.hasText(incomingTraceId)
                ? incomingTraceId
                : UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        MDC.put(MDC_KEY, traceId);
        MDC.put("method", httpRequest.getMethod());
        MDC.put("uri",    httpRequest.getRequestURI());

        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        log.trace("→ {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            log.trace("← {} {} status={}", httpRequest.getMethod(),
                    httpRequest.getRequestURI(), httpResponse.getStatus());
            MDC.clear();
        }
    }
}