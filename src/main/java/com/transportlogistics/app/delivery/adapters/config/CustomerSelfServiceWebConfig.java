package com.transportlogistics.app.delivery.adapters.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
public class CustomerSelfServiceWebConfig implements WebMvcConfigurer {
    private final String origin;
    public CustomerSelfServiceWebConfig(@Value("${app.delivery.self-service.customer-origin}") String origin) { this.origin = origin; }
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/public/v1/delivery-self-service/**").allowedOrigins(origin)
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key").maxAge(300);
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                               jakarta.servlet.http.HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Referrer-Policy", "no-referrer");
                return true;
            }
        }).addPathPatterns("/public/v1/delivery-self-service/**");
    }
}
