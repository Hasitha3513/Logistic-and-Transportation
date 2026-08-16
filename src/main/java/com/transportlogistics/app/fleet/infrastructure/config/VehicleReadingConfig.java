package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingEventPublisher;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleReadingTransaction;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.application.service.VehicleReadingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class VehicleReadingConfig {
    @Bean
    VehicleReadingService vehicleReadingService(VehicleRepository vehicles, VehicleReadingRepository readings,
                                                VehicleReadingTransaction transactions,
                                                VehicleReadingEventPublisher events, Clock clock) {
        return new VehicleReadingService(vehicles, readings, transactions, events, clock);
    }
}
