package com.transportlogistics.app.fleet.infrastructure.config;

import com.transportlogistics.app.fleet.application.ports.in.VehicleCategoryUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleCategoryRepository;
import com.transportlogistics.app.fleet.application.service.VehicleCategoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VehicleCategoryConfig {
    @Bean
    VehicleCategoryUseCase vehicleCategoryUseCase(VehicleCategoryRepository repo) {
        return new VehicleCategoryService(repo);
    }
}
