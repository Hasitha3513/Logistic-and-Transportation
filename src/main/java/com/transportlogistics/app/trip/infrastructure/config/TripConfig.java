package com.transportlogistics.app.trip.infrastructure.config;

import com.transportlogistics.app.trip.VehicleAllocationLookup;
import com.transportlogistics.app.trip.DriverAssignmentLookup;
import com.transportlogistics.app.trip.TripFuelContextLookup;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.TripRepository;
import com.transportlogistics.app.trip.application.ports.out.VehicleEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.DriverEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.RouteEligibilityPort;
import com.transportlogistics.app.trip.application.ports.out.TripHistoryRepository;
import com.transportlogistics.app.trip.application.ports.out.TripTransaction;
import com.transportlogistics.app.trip.application.ports.out.TripDispatchRepository;
import com.transportlogistics.app.trip.application.service.TripService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class TripConfig {
    @Bean
    TripUseCase tripUseCase(TripRepository r, VehicleEligibilityPort vehicleEligibility,
                            DriverEligibilityPort driverEligibility, RouteEligibilityPort routeEligibility,
                            TripHistoryRepository history,
                            TripTransaction transactions, TripDispatchRepository dispatches) {
        return new TripService(r, vehicleEligibility, driverEligibility, routeEligibility, history, transactions, dispatches,
                Clock.systemUTC());
    }

    @Bean
    VehicleAllocationLookup vehicleAllocationLookup(TripRepository trips) {
        return trips::hasOverlappingVehicleAllocation;
    }

    @Bean
    DriverAssignmentLookup driverAssignmentLookup(TripRepository trips) {
        return trips::hasOverlappingDriverAssignment;
    }

    @Bean
    TripFuelContextLookup tripFuelContextLookup(TripRepository trips) {
        return tripId -> trips.findById(tripId).map(trip -> new TripFuelContextLookup.TripFuelContext(
                trip.id(), trip.tripNumber(), trip.status(), trip.vehicleId(), trip.driverId(),
                trip.requestedStartTime(), trip.requestedEndTime()));
    }
}
