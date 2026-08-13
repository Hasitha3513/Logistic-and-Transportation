package com.transportlogistics.app.trip.infrastructure.config;

import com.transportlogistics.app.trip.VehicleAllocationLookup;
import com.transportlogistics.app.trip.DriverAssignmentLookup;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.service.TripService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TripConfig {
    @Bean
    TripUseCase tripUseCase(TripRepository r, VehicleEligibilityPort vehicleEligibility,
                            DriverEligibilityPort driverEligibility) {
        return new TripService(r, vehicleEligibility, driverEligibility);
    }

    @Bean
    VehicleAllocationLookup vehicleAllocationLookup(TripRepository trips) {
        return trips::hasOverlappingVehicleAllocation;
    }

    @Bean
    DriverAssignmentLookup driverAssignmentLookup(TripRepository trips) {
        return trips::hasOverlappingDriverAssignment;
    }
}
