package com.transportlogistics.app.tracking.application.telemetry;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Provider-neutral position fact shared by retained V1/V2 and gated additive V3 contracts. */
public interface CanonicalTelemetryEvent {
    UUID eventId();
    String eventType();
    int eventVersion();
    UUID tenantId();
    UUID vehicleId();
    UUID deviceId();
    String providerAlias();
    String providerMessageId();
    String dedupeIdentity();
    BigDecimal latitude();
    BigDecimal longitude();
    BigDecimal speedKph();
    BigDecimal headingDegrees();
    BigDecimal horizontalAccuracyMeters();
    BigDecimal altitudeMeters();
    EngineState engineState();
    BigDecimal odometerKm();
    BigDecimal engineHours();
    Instant recordedAt();
    Instant receivedAt();
}
