package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedAttribution;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpeedAttributionLookupPort {
    Optional<SpeedAttribution> findAt(UUID tenantId, UUID vehicleId, Instant sourceTimestamp);
}
