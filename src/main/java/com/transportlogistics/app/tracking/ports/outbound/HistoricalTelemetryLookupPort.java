package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface HistoricalTelemetryLookupPort {
    Optional<HistoricalTelemetry> findExact(UUID tenantId, Instant sourceTimestamp, UUID historyId);
}
