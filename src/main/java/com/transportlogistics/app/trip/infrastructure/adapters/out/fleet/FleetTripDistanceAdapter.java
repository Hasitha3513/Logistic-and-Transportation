package com.transportlogistics.app.trip.infrastructure.adapters.out.fleet;

import com.transportlogistics.app.fleet.VehicleMileageQuery;
import com.transportlogistics.app.trip.application.ports.out.TripDistancePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class FleetTripDistanceAdapter implements TripDistancePort {
    private final VehicleMileageQuery vehicleMileageQuery;

    @Override
    public DistanceResult getTripDistance(UUID tripId, UUID vehicleId) {
        var summary = vehicleMileageQuery.getTripDistance(tripId, vehicleId);
        return new DistanceResult(
                summary.tripId(),
                summary.vehicleId(),
                summary.startOdometer(),
                summary.endOdometer(),
                summary.distanceKm(),
                summary.status().name(),
                summary.meterResetEncountered(),
                summary.notes()
        );
    }
}
