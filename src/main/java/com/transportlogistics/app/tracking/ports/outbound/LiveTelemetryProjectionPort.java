package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.application.telemetry.LiveTelemetryProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveTelemetryProjectionPort {
    ProjectionResult project(LiveTelemetryProjection projection);

    Optional<LiveTelemetryProjection> find(UUID tenantId, UUID vehicleId);

    List<LiveTelemetryProjection> findLive(UUID tenantId, Instant now, int limit);

    enum ProjectionResult {
        UPDATED,
        DUPLICATE,
        STALE
    }
}
