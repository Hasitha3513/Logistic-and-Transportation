package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.out.*;
import com.transportlogistics.app.fleet.application.service.VehicleReadingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class VehicleReadingConfig {
    @Bean
    VehicleReadingService vehicleReadingService(VehicleRepository vehicles, VehicleReadingRepository readings,
                                                VehicleMeterResetRepository resets,
                                                VehicleReadingTransaction transactions,
                                                VehicleReadingEventPublisher events, Clock clock) {
        return new VehicleReadingService(vehicles, readings, resets, transactions, events, clock);
    }
}
