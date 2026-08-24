package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.DriverViolationUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverViolationRepository;
import com.transportlogistics.app.fleet.application.service.DriverViolationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverViolationConfig {

    @Bean
    DriverViolationUseCase driverViolationUseCase(DriverViolationRepository violations, DriverRepository drivers) {
        return new DriverViolationService(violations, drivers);
    }
}
