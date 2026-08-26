package com.transportlogistics.app.system.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.VehicleAllocationAvailability;
import com.transportlogistics.app.trip.VehicleAllocationLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class TripVehicleAllocationAdapter implements VehicleAllocationAvailability {
    private final VehicleAllocationLookup trips;

    @Override
    public boolean hasOverlap(UUID vehicleId, OffsetDateTime from, OffsetDateTime to, UUID excludeTripId) {
        return trips.hasOverlap(vehicleId, from, to, excludeTripId);
    }
}
