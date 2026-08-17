package com.transportlogistics.app.trip.infrastructure.config;

import com.transportlogistics.app.trip.DriverAssignmentLookup;
import com.transportlogistics.app.trip.TripFuelContextLookup;
import com.transportlogistics.app.trip.VehicleAllocationLookup;
import com.transportlogistics.app.trip.application.ports.in.TripUseCase;
import com.transportlogistics.app.trip.application.ports.out.*;
import com.transportlogistics.app.trip.application.service.TripService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class TripConfig {
    @Bean
    TripUseCase tripUseCase(TripRepository r, VehicleEligibilityPort vehicleEligibility,
                            DriverEligibilityPort driverEligibility, RouteEligibilityPort routeEligibility,
                            TripVehicleReadingPort vehicleReadings, TripActorPort actors,
                            com.transportlogistics.app.trip.application.ports.out.TripDistancePort distancePort,
                            TripHistoryRepository history,
                            TripTransaction transactions, TripDispatchRepository dispatches) {
        return new TripService(r, vehicleEligibility, driverEligibility, routeEligibility, vehicleReadings,
                actors, distancePort, history, transactions, dispatches, Clock.systemUTC());
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
