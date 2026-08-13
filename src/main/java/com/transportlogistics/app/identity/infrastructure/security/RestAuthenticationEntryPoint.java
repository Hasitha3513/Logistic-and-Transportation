package com.transportlogistics.app.identity.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityErrorWriter errors;

    RestAuthenticationEntryPoint(SecurityErrorWriter errors) {
        this.errors = errors;
    }

    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        errors.write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
    }
}
