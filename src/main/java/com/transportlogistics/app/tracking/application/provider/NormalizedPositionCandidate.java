package com.transportlogistics.app.tracking.application.provider;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.BatteryChargingState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.ExternalPowerState;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV2.TamperState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record NormalizedPositionCandidate(
        String externalDeviceReference,
        Instant sourceTimestamp,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal horizontalAccuracyMeters,
        BigDecimal speedKph,
        BigDecimal headingDegrees,
        BigDecimal altitudeMeters,
        EngineState engineState,
        BigDecimal odometerKm,
        BigDecimal engineHours,
        String providerMessageId,
        Long providerSequence,
        TamperState tamperState,
        BigDecimal batteryLevelPercent,
        BigDecimal batteryVoltageVolts,
        ExternalPowerState externalPowerState,
        BatteryChargingState batteryChargingState) {

    public NormalizedPositionCandidate(
            String externalDeviceReference, Instant sourceTimestamp, BigDecimal latitude,
            BigDecimal longitude, BigDecimal horizontalAccuracyMeters, BigDecimal speedKph,
            BigDecimal headingDegrees, BigDecimal altitudeMeters, EngineState engineState,
            BigDecimal odometerKm, BigDecimal engineHours, String providerMessageId,
            Long providerSequence) {
        this(externalDeviceReference, sourceTimestamp, latitude, longitude,
                horizontalAccuracyMeters, speedKph, headingDegrees, altitudeMeters, engineState,
                odometerKm, engineHours, providerMessageId, providerSequence,
                null, null, null, null, null);
    }

    public NormalizedPositionCandidate {
        if (externalDeviceReference == null || externalDeviceReference.isBlank()
                || externalDeviceReference.length() > 160) {
            throw new IllegalArgumentException("External device reference is invalid");
        }
        Objects.requireNonNull(sourceTimestamp, "sourceTimestamp");
        requireRange(latitude, "latitude", "-90", "90");
        requireRange(longitude, "longitude", "-180", "180");
        nonNegative(horizontalAccuracyMeters, "horizontalAccuracyMeters");
        nonNegative(speedKph, "speedKph");
        if (headingDegrees != null && (headingDegrees.signum() < 0
                || headingDegrees.compareTo(BigDecimal.valueOf(360)) >= 0)) {
            throw new IllegalArgumentException("headingDegrees must be in [0,360)");
        }
        nonNegative(odometerKm, "odometerKm");
        nonNegative(engineHours, "engineHours");
        validateSignal(batteryLevelPercent, new BigDecimal("100.0"), 3, "batteryLevelPercent");
        validateSignal(batteryVoltageVolts, new BigDecimal("1000.0"), 6, "batteryVoltageVolts");
        if (providerMessageId != null && (providerMessageId.isBlank()
                || providerMessageId.length() > 160)) {
            throw new IllegalArgumentException("providerMessageId is invalid");
        }
        engineState = engineState == null ? EngineState.UNKNOWN : engineState;
    }

    private static void requireRange(BigDecimal value, String field, String minimum, String maximum) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(new BigDecimal(minimum)) < 0
                || value.compareTo(new BigDecimal(maximum)) > 0) {
            throw new IllegalArgumentException(field + " is outside WGS84 bounds");
        }
    }

    private static void nonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
    }

    private static void validateSignal(
            BigDecimal value, BigDecimal maximum, int scale, String field) {
        if (value != null && (value.signum() < 0 || value.compareTo(maximum) > 0
                || Math.max(value.scale(), 0) > scale)) {
            throw new IllegalArgumentException(field + " is invalid");
        }
    }
}
