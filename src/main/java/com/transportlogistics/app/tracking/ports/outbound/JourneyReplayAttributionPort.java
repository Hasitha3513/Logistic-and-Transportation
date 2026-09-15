package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.journeyreplay.JourneyReplayModels.Attribution;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JourneyReplayAttributionPort {
    Optional<Attribution> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp);
}
