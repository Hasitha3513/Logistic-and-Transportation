package com.transportlogistics.app.tracking.adapters.outbound.trip;

import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationAssignmentLookupPort;
import com.transportlogistics.app.trip.VehicleTripAssignmentLookup;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripRouteDeviationAssignmentAdapter implements RouteDeviationAssignmentLookupPort {
    private final VehicleTripAssignmentLookup assignments;

    TripRouteDeviationAssignmentAdapter(VehicleTripAssignmentLookup assignments) {
        this.assignments = assignments;
    }

    @Override
    public Optional<Assignment> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp) {
        return assignments.findAt(tenantId, vehicleId, sourceTimestamp)
                .map(value -> new Assignment(value.tripId(), value.driverId(),
                        value.routeId(), value.routeVersion()));
    }
}
