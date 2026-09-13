package com.transportlogistics.app.tracking.application.telemetry;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Versioned, provider-neutral transport contract for the Tracking-local Kafka stream. */
public record TrackingTelemetryIngestedV1(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID tenantId,
        UUID vehicleId,
        UUID deviceId,
        String providerAlias,
        String providerMessageId,
        String dedupeIdentity,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speedKph,
        BigDecimal headingDegrees,
        BigDecimal horizontalAccuracyMeters,
        BigDecimal altitudeMeters,
        EngineState engineState,
        BigDecimal odometerKm,
        BigDecimal engineHours,
        Instant recordedAt,
        Instant receivedAt) {
    public static final String TYPE = "TRACKING_TELEMETRY_INGESTED_V1";
    public static final int VERSION = 1;

    public TrackingTelemetryIngestedV1 {
        Objects.requireNonNull(eventId, "eventId");
        if (!TYPE.equals(eventType) || eventVersion != VERSION) {
            throw new IllegalArgumentException("Unsupported telemetry event version");
        }
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }
}
