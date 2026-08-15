package com.transportlogistics.app.fuel.infrastructure.config;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.application.ports.out.FuelActorPort;
import com.transportlogistics.app.fuel.application.ports.out.FuelEventPublisher;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueHistoryRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelIssueRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelLimitPolicyRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelStationRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelTransaction;
import com.transportlogistics.app.fuel.application.ports.out.FuelVoucherGenerator;
import com.transportlogistics.app.fuel.application.ports.out.TripFuelContextPort;
import com.transportlogistics.app.fuel.application.ports.out.VehicleFuelContextPort;
import com.transportlogistics.app.fuel.application.service.FuelIssueService;
import com.transportlogistics.app.fuel.application.service.FuelStationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class FuelConfig {
    @Bean
    FuelIssueUseCase fuelIssueUseCase(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                                      FuelStationRepository stations, FuelLimitPolicyRepository limits,
                                      VehicleFuelContextPort vehicles, TripFuelContextPort trips,
                                      FuelActorPort actors, FuelVoucherGenerator vouchers,
                                      FuelTransaction transactions, FuelEventPublisher events, Clock clock) {
        return new FuelIssueService(issues, history, stations, limits, vehicles, trips, actors, vouchers,
                transactions, events, clock);
    }

    @Bean
    FuelStationUseCase fuelStationUseCase(FuelStationRepository stations) {
        return new FuelStationService(stations);
    }
}
