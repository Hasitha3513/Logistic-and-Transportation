package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.VehicleDispatchEligibility;
import com.transportlogistics.app.fleet.application.ports.in.VehicleUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.application.service.VehicleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VehicleConfig {
    @Bean
    VehicleUseCase vehicleUseCase(VehicleRepository repo, VehicleDocumentRepository documents) {
        return new VehicleService(repo, documents);
    }

    @Bean
    VehicleDispatchEligibility vehicleDispatchEligibility(VehicleUseCase vehicles) {
        return vehicles::assertAvailableForDispatch;
    }
}
