package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.DriverLicenseUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.service.DriverLicenseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverLicenseConfig {
    @Bean
    DriverLicenseUseCase driverLicenseUseCase(DriverRepository drivers, DriverLicenseRepository licenses) {
        return new DriverLicenseService(drivers, licenses);
    }
}
