package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.DriverAssignmentEligibility;
import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.fleet.application.ports.in.DriverAvailabilityUseCase;
import com.transportlogistics.app.fleet.application.ports.in.DriverUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverLicenseRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.service.DriverService;
import com.transportlogistics.app.fleet.application.service.DriverAvailabilityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverConfig {
    @Bean
    DriverUseCase driverUseCase(DriverRepository repo) {
        return new DriverService(repo);
    }

    @Bean
    DriverAvailabilityUseCase driverAvailabilityUseCase(DriverRepository drivers, DriverLicenseRepository licenses,
                                                         DriverAssignmentAvailability assignments) {
        return new DriverAvailabilityService(drivers, licenses, assignments);
    }

    @Bean
    DriverAssignmentEligibility driverAssignmentEligibility(DriverAvailabilityUseCase availability) {
        return (driverId, requiredClass, from, to) -> {
            var result = availability.evaluate(new DriverAvailabilityUseCase.Query(driverId, from, to,
                    requiredClass, null, false, true));
            if (!result.available()) {
                var codes = result.reasons().stream().map(reason -> reason.code().name()).toList();
                throw new IllegalArgumentException("Driver is unavailable for assignment: " + codes);
            }
        };
    }
}
