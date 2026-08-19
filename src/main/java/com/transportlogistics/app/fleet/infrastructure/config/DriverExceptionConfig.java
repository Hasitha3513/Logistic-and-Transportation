package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.fleet.application.ports.in.DriverExceptionUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverExceptionRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.service.DriverExceptionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverExceptionConfig {

    @Bean
    DriverExceptionUseCase driverExceptionUseCase(
            DriverExceptionRepository driverExceptions,
            DriverRepository drivers,
            DriverAssignmentAvailability assignments
    ) {
        return new DriverExceptionService(driverExceptions, drivers, assignments);
    }
}
