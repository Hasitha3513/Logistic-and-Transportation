package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.VehicleMileageQuery;
import com.transportlogistics.app.fleet.VehicleReadingRecorder;
import com.transportlogistics.app.fleet.application.ports.in.VehicleReadingUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleMeterResetRepository;
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
                                                VehicleMeterResetRepository meterResets,
                                                VehicleReadingTransaction transactions,
                                                VehicleReadingEventPublisher events, Clock clock) {
        return new VehicleReadingService(vehicles, readings, meterResets, transactions, events, clock);
    }
}