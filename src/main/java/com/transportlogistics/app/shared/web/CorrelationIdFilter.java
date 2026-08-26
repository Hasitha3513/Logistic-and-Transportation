package com.transportlogistics.app.shared.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {
    public static final String HEADER = "X-Correlation-ID";
    public static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws ServletException, IOException {
        var request = (HttpServletRequest) servletRequest;
        var response = (HttpServletResponse) servletResponse;
        var supplied = request.getHeader(HEADER);
        var correlationId = supplied == null || supplied.isBlank() ? UUID.randomUUID().toString() : supplied.trim();
        request.setAttribute(ATTRIBUTE, correlationId);
        response.setHeader(HEADER, correlationId);
        MDC.put("correlationId", correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
