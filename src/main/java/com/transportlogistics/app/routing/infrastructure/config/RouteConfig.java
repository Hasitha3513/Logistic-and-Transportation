package com.transportlogistics.app.routing.infrastructure.config;

import com.transportlogistics.app.routing.application.ports.in.RouteUseCase;
import com.transportlogistics.app.routing.application.ports.out.RouteRepository;
import com.transportlogistics.app.routing.application.service.RouteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RouteConfig {
    @Bean
    RouteUseCase routeUseCase(RouteRepository repo) {
        return new RouteService(repo);
    }
}
