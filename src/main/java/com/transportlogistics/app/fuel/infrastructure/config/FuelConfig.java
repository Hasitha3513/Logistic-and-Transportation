package com.transportlogistics.app.fuel.infrastructure.config;

import com.transportlogistics.app.fuel.application.ports.in.FuelIssueUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelStationUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPurchaseUseCase;
import com.transportlogistics.app.fuel.application.ports.in.FuelPriceUseCase;
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
import com.transportlogistics.app.fuel.application.ports.out.FuelPriceRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseHistoryRepository;
import com.transportlogistics.app.fuel.application.ports.out.FuelPurchaseNumberGenerator;
import com.transportlogistics.app.fuel.application.ports.out.FuelVendorPort;
import com.transportlogistics.app.fuel.application.service.FuelIssueService;
import com.transportlogistics.app.fuel.application.service.FuelStationService;
import com.transportlogistics.app.fuel.application.service.FuelPurchaseService;
import com.transportlogistics.app.fuel.application.service.FuelPriceService;
import com.transportlogistics.app.fuel.domain.service.FuelPurchasePolicy;
import com.transportlogistics.app.fuel.application.ports.out.FuelVehicleReadingPort;
import com.transportlogistics.app.fuel.application.ports.out.BunkerStockLedgerRepository;
import com.transportlogistics.app.fuel.application.ports.out.BunkerTankRepository;
import com.transportlogistics.app.fuel.domain.policy.BunkerTankPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class FuelConfig {
    @Bean
    BunkerTankPolicy bunkerTankPolicy() {
        return new BunkerTankPolicy();
    }

    @Bean
    FuelIssueUseCase fuelIssueUseCase(FuelIssueRepository issues, FuelIssueHistoryRepository history,
                                      FuelStationRepository stations, FuelLimitPolicyRepository limits,
                                      VehicleFuelContextPort vehicles, TripFuelContextPort trips,
                                      FuelActorPort actors, FuelVoucherGenerator vouchers,
                                      FuelTransaction transactions, FuelEventPublisher events,
                                      FuelVehicleReadingPort readings, FuelPriceRepository fuelPrices,
                                      BunkerTankRepository bunkerTanks, BunkerStockLedgerRepository bunkerMovements,
                                      Clock clock) {
        return new FuelIssueService(issues, history, stations, limits, vehicles, trips, actors, vouchers,
                transactions, events, fuelPrices, readings, bunkerTanks, bunkerMovements, clock);
    }

    @Bean
    FuelStationUseCase fuelStationUseCase(FuelStationRepository stations) {
        return new FuelStationService(stations);
    }

    @Bean
    FuelPurchasePolicy fuelPurchasePolicy() {
        return new FuelPurchasePolicy();
    }

    @Bean
    FuelPriceUseCase fuelPriceUseCase(FuelPriceRepository prices, FuelVendorPort vendors,
                                      FuelPurchasePolicy policy, Clock clock) {
        return new FuelPriceService(prices, vendors, policy, clock);
    }

    @Bean
    FuelPurchaseUseCase fuelPurchaseUseCase(FuelPurchaseRepository purchases,
                                            FuelPurchaseHistoryRepository history,
                                            FuelPriceRepository prices, FuelStationRepository stations,
                                            FuelVendorPort vendors, FuelActorPort actors,
                                            FuelPurchaseNumberGenerator numbers, FuelTransaction transactions,
                                            FuelEventPublisher events, FuelPurchasePolicy policy,
                                            BunkerTankRepository bunkerTanks, BunkerStockLedgerRepository bunkerMovements,
                                            BunkerTankPolicy bunkerTankPolicy, Clock clock) {
        return new FuelPurchaseService(purchases, history, prices, stations, vendors, actors, numbers,
                transactions, events, policy, bunkerTanks, bunkerMovements, bunkerTankPolicy, clock);
    }
}
