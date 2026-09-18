package com.transportlogistics.app.tracking.application.telemetry;

import com.transportlogistics.app.tracking.domain.TrackingModels.EngineState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Additive V3 canonical telemetry contract with distinct ignition and authoritative
 * engine-running observations. Production publication remains disabled until V3 persistence is
 * available.
 */
public record TrackingTelemetryIngestedV3(
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
        TrackingTelemetryIngestedV2.TamperState tamperState,
        BigDecimal batteryLevelPercent,
        BigDecimal batteryVoltageVolts,
        TrackingTelemetryIngestedV2.ExternalPowerState externalPowerState,
        TrackingTelemetryIngestedV2.BatteryChargingState batteryChargingState,
        IgnitionState ignitionState,
        EngineRunningState engineRunningState,
        EngineRunningSource engineRunningSource) implements CanonicalTelemetryEvent {

    public static final String TYPE = TrackingTelemetryIngestedV1.TYPE;
    public static final int VERSION = 3;
    public static final String TOPIC = "tracking.telemetry.ingested.v3";
    public static final String DEAD_LETTER_TOPIC = "tracking.telemetry.ingested.v3.dlt";

    public TrackingTelemetryIngestedV3 {
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
        if ((engineRunningState == null) != (engineRunningSource == null)) {
            throw new IllegalArgumentException(
                    "engineRunningState and engineRunningSource must be present together");
        }
        if (ignitionState != null && engineState != null
                && !ignitionState.name().equals(engineState.name())) {
            throw new IllegalArgumentException(
                    "Legacy engineState must retain ignition semantics");
        }
    }

    private static void validateDecimal(
            BigDecimal value, BigDecimal minimum, BigDecimal maximum, int scale, String field) {
        if (value != null && (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0
                || Math.max(value.scale(), 0) > scale)) {
            throw new IllegalArgumentException("Invalid " + field);
        }
    }

    public enum IgnitionState { ON, OFF, UNKNOWN }
    public enum EngineRunningState { RUNNING, NOT_RUNNING, UNKNOWN }
    public enum EngineRunningSource {
        DEVICE_NATIVE_CAN,
        DEVICE_NATIVE_RPM,
        DEVICE_NATIVE_STATUS,
        PROVIDER_VERIFIED_DERIVATION
    }
}
