package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Attribution;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.AttributionInterval;
import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.ReplayScope;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JourneyReplayAttributionPort {
    Optional<Attribution> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp);
    Optional<ReplayScope> findReplayScope(UUID tenantId, UUID tripId);
    List<AttributionInterval> findOverlapping(
            UUID tenantId, UUID vehicleId, Instant rangeStart, Instant rangeEnd);
}
