package com.transportlogistics.app.tracking.application.telemetry;

import java.time.Instant;
import java.util.Objects;

/** Rebuildable live-state projection of the canonical TS02 event. */
public record LiveTelemetryProjection(
        CanonicalTelemetryEvent telemetry,
        Instant projectedAt) {
    public LiveTelemetryProjection {
        Objects.requireNonNull(telemetry, "telemetry");
        Objects.requireNonNull(projectedAt, "projectedAt");
    }
}
