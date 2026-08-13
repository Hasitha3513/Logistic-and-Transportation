package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.DriverUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.service.DriverService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverConfig {
    @Bean
    DriverUseCase driverUseCase(DriverRepository repo) {
        return new DriverService(repo);
    }
}
