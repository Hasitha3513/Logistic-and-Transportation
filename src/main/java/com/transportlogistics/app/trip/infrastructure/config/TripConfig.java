package com.transportlogistics.app.trip.infrastructure.config;

import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.service.TripService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TripConfig {
    @Bean
    TripUseCase tripUseCase(TripRepository r, VehicleEligibilityPort vehicleEligibility) {
        return new TripService(r, vehicleEligibility);
    }
}
