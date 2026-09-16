package com.transportlogistics.app.tracking.application.provider;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.BatteryChargingState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.ExternalPowerState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.TamperState;
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
        EngineState engineState,
        TamperState tamperState,
        BigDecimal batteryLevelPercent,
        BigDecimal batteryVoltageVolts,
        ExternalPowerState externalPowerState,
        BatteryChargingState batteryChargingState) {

    public NormalizedTelemetryPoint(
            UUID tenantId, String externalDeviceReference, String providerMessageId,
            Instant recordedAt, BigDecimal latitude, BigDecimal longitude, BigDecimal speedKph,
            BigDecimal headingDegrees, BigDecimal horizontalAccuracyMeters, BigDecimal odometerKm,
            EngineState engineState) {
        this(tenantId, externalDeviceReference, providerMessageId, recordedAt, latitude, longitude,
                speedKph, headingDegrees, horizontalAccuracyMeters, odometerKm, engineState,
                null, null, null, null, null);
    }

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
        validateSignal(batteryLevelPercent, new BigDecimal("100.0"), 3, "battery level");
        validateSignal(batteryVoltageVolts, new BigDecimal("1000.0"), 6, "battery voltage");
    }

    private static void validateSignal(BigDecimal value, BigDecimal maximum, int scale, String name) {
        if (value != null && (value.signum() < 0 || value.compareTo(maximum) > 0
                || Math.max(value.scale(), 0) > scale)) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }
}
