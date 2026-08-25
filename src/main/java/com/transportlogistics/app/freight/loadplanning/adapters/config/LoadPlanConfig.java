package com.transportlogistics.app.freight.loadplanning.adapters.config;

import com.transportlogistics.app.freight.loadplanning.application.LoadPlanService;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.CargoManifestLookupPort;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.LoadPlanUseCase;
import com.transportlogistics.app.freight.loadplanning.ports.inbound.VehicleLoadSpaceLookupPort;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanEventPublisher;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanNumberGenerator;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanRepository;
import com.transportlogistics.app.freight.loadplanning.ports.outbound.LoadPlanTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class LoadPlanConfig {

    @Bean
    LoadPlanUseCase loadPlanUseCase(LoadPlanRepository repository,
                                   LoadPlanNumberGenerator numberGenerator,
                                   CargoManifestLookupPort manifestLookup,
                                   VehicleLoadSpaceLookupPort vehicleLookup,
                                   LoadPlanEventPublisher eventPublisher,
                                   LoadPlanTransaction transactions,
                                   Clock clock) {
        return new LoadPlanService(
                repository,
                numberGenerator,
                manifestLookup,
                vehicleLookup,
                eventPublisher,
                transactions,
                clock
        );
    }
}
