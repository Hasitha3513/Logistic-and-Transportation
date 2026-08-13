package com.transportlogistics.app.system.infrastructure.adapters.out;

import com.transportlogistics.app.fleet.DriverAssignmentAvailability;
import com.transportlogistics.app.trip.DriverAssignmentLookup;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class TripDriverAssignmentAdapter implements DriverAssignmentAvailability {
    private final DriverAssignmentLookup trips;

    TripDriverAssignmentAdapter(DriverAssignmentLookup trips) {
        this.trips = trips;
    }

    @Override
    public boolean hasOverlap(UUID driverId, OffsetDateTime from, OffsetDateTime to, UUID excludeTripId) {
        return trips.hasOverlap(driverId, from, to, excludeTripId);
    }
}
