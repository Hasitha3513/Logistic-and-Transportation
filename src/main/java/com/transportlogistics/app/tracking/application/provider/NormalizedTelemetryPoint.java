package com.transportlogistics.app.tracking.application.provider;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NormalizedTelemetryPoint(
        UUID tenantId,
        String externalDeviceReference,
        String providerMessageId,
        Instant recordedAt,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speedKph,
        BigDecimal headingDegrees,
        BigDecimal horizontalAccuracyMeters,
        BigDecimal odometerKm,
        EngineState engineState) {

    public NormalizedTelemetryPoint {
        Objects.requireNonNull(tenantId, "tenantId");
        var validated = new NormalizedPositionCandidate(
                externalDeviceReference,
                recordedAt,
                latitude,
                longitude,
                horizontalAccuracyMeters,
                speedKph,
                headingDegrees,
                null,
                engineState,
                odometerKm,
                null,
                providerMessageId,
                null);
        externalDeviceReference = validated.externalDeviceReference();
        engineState = validated.engineState();
    }
}
