package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV1;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HistoricalTelemetryStorePort {
    BatchResult persist(List<TrackingTelemetryIngestedV1> telemetry);

    List<HistoricalTelemetry> find(
            UUID tenantId, UUID vehicleId, Instant fromInclusive, Instant toExclusive, int limit);

    record BatchResult(int persisted, int duplicate, int reduced) {
    }
}
