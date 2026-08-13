package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.application.ports.in.LocationUseCase;
import com.transportlogistics.app.organization.application.ports.out.LocationRepository;
import com.transportlogistics.app.organization.application.service.LocationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LocationConfig {
    @Bean
    LocationUseCase locationUseCase(LocationRepository repo) {
        return new LocationService(repo);
    }
}
