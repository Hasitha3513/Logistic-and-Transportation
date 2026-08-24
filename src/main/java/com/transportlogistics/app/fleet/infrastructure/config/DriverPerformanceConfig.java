package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.DriverTripMetricsProvider;
import com.transportlogistics.app.fleet.application.ports.in.DriverPerformanceUseCase;
import com.transportlogistics.app.fleet.application.ports.out.DriverRepository;
import com.transportlogistics.app.fleet.application.ports.out.DriverTripMetricsPort;
import com.transportlogistics.app.fleet.application.ports.out.DriverViolationRepository;
import com.transportlogistics.app.fleet.application.service.DriverPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DriverPerformanceConfig {

    @Bean
    DriverPerformanceUseCase driverPerformanceUseCase(
            DriverRepository drivers,
            DriverViolationRepository violations,
            @Autowired(required = false) DriverTripMetricsProvider tripMetrics
    ) {
        DriverTripMetricsPort metricsPort = tripMetrics != null
                ? tripMetrics::getTripSummary
                : driverId -> new DriverTripMetricsProvider.DriverTripSummary(0, 0, 0);
        return new DriverPerformanceService(drivers, violations, metricsPort);
    }
}
