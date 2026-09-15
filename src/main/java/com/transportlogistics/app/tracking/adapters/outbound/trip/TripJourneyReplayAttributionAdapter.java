package com.transportlogistics.app.tracking.adapters.outbound.trip;

import com.transportlogistics.app.trip.TripReplayQuery;
import com.transportlogistics.app.trip.VehicleTripAssignmentLookup;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Attribution;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionInterval;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionStatus;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayScope;
import com.transportlogistics.app.tracking.ports.outbound.JourneyReplayAttributionPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class TripJourneyReplayAttributionAdapter implements JourneyReplayAttributionPort {
    private final VehicleTripAssignmentLookup pointLookup;
    private final TripReplayQuery replayQuery;

    TripJourneyReplayAttributionAdapter(
            VehicleTripAssignmentLookup pointLookup, TripReplayQuery replayQuery) {
        this.pointLookup = pointLookup;
        this.replayQuery = replayQuery;
    }

    @Override
    public Optional<Attribution> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp) {
        return pointLookup.findAt(tenantId, vehicleId, sourceTimestamp).map(value ->
                new Attribution(value.tripId(), value.routeId(), value.routeVersion(),
                        AttributionStatus.ATTRIBUTED));
    }

    @Override
    public Optional<ReplayScope> findReplayScope(UUID tenantId, UUID tripId) {
        return replayQuery.findReplayScope(tenantId, tripId).map(value -> new ReplayScope(
                value.tripId(), value.vehicleId(), value.actualStartTime(), value.actualEndTime(),
                value.tripStatus(), value.routeId(), value.routeVersion()));
    }

    @Override
    public List<AttributionInterval> findOverlapping(
            UUID tenantId, UUID vehicleId, Instant rangeStart, Instant rangeEnd) {
        return replayQuery.findAssignmentsOverlapping(tenantId, vehicleId, rangeStart, rangeEnd)
                .stream().map(value -> new AttributionInterval(value.tripId(), value.effectiveStart(),
                        value.effectiveEnd(), value.routeId(), value.routeVersion())).toList();
    }
}
