package com.transportlogistics.app.tracking.adapters.outbound.trip;

import com.transportlogistics.app.tracking.domain.speed.SpeedAttribution;
import com.transportlogistics.app.tracking.ports.outbound.SpeedAttributionLookupPort;
import com.transportlogistics.app.trip.VehicleTripAssignmentLookup;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TripSpeedAttributionAdapter implements SpeedAttributionLookupPort {
    private final VehicleTripAssignmentLookup assignments;

    TripSpeedAttributionAdapter(VehicleTripAssignmentLookup assignments) {
        this.assignments = assignments;
    }

    @Override
    public Optional<SpeedAttribution> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp) {
        return assignments.findAt(tenantId, vehicleId, sourceTimestamp)
                .map(value -> new SpeedAttribution(value.tripId(), value.driverId(),
                        value.routeId(), value.routeVersion()));
    }
}
