package com.transportlogistics.app.tracking.application.telemetry;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Framework-neutral immutable historical telemetry fact. */
public record HistoricalTelemetry(
        UUID eventId,
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
        Instant receivedAt,
        Trust trust,
        String quality,
        Ordering ordering) {
}
