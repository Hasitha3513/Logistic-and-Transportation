package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.VehicleDocumentUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.vehiclemaster.ports.outbound.VehicleRepository;
import com.transportlogistics.app.fleet.application.service.VehicleDocumentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VehicleDocumentConfig {
    @Bean
    VehicleDocumentUseCase vehicleDocumentUseCase(VehicleRepository vehicles, VehicleDocumentRepository documents) {
        return new VehicleDocumentService(vehicles, documents);
    }
}
