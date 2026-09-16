package com.transportlogistics.app.tracking.application.telemetry;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Additive V2 canonical telemetry contract; absent optional signals mean not reported. */
public record TrackingTelemetryIngestedV2(
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
        Instant receivedAt,
        TamperState tamperState,
        BigDecimal batteryLevelPercent,
        BigDecimal batteryVoltageVolts,
        ExternalPowerState externalPowerState,
        BatteryChargingState batteryChargingState) implements CanonicalTelemetryEvent {

    public static final String TYPE = TrackingTelemetryIngestedV1.TYPE;
    public static final int VERSION = 2;

    public TrackingTelemetryIngestedV2 {
        Objects.requireNonNull(eventId, "eventId");
        if (!TYPE.equals(eventType) || eventVersion != VERSION) {
            throw new IllegalArgumentException("Unsupported telemetry event version");
        }
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(receivedAt, "receivedAt");
        validateDecimal(batteryLevelPercent, BigDecimal.ZERO, new BigDecimal("100.0"), 3,
                "batteryLevelPercent");
        validateDecimal(batteryVoltageVolts, BigDecimal.ZERO, new BigDecimal("1000.0"), 6,
                "batteryVoltageVolts");
    }

    private static void validateDecimal(
            BigDecimal value, BigDecimal minimum, BigDecimal maximum, int scale, String field) {
        if (value != null && (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0
                || Math.max(value.scale(), 0) > scale)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
    }

    public enum TamperState { DETECTED, CLEAR, UNKNOWN }
    public enum ExternalPowerState { CONNECTED, DISCONNECTED, UNKNOWN }
    public enum BatteryChargingState { CHARGING, NOT_CHARGING, UNKNOWN }
}
