package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.VehicleTypeUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleTypeRepository;
import com.transportlogistics.app.fleet.application.service.VehicleTypeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VehicleTypeConfig {
    @Bean
    VehicleTypeUseCase vehicleTypeUseCase(VehicleTypeRepository repo) {
        return new VehicleTypeService(repo);
    }
}
