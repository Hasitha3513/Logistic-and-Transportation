package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.application.telemetry.CanonicalTelemetryEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HistoricalTelemetryStorePort {
    BatchResult persist(List<? extends CanonicalTelemetryEvent> telemetry);

    List<HistoricalTelemetry> find(
            UUID tenantId, UUID vehicleId, Instant fromInclusive, Instant toExclusive, int limit);

    record BatchResult(int persisted, int duplicate, int reduced) {
    }
}
