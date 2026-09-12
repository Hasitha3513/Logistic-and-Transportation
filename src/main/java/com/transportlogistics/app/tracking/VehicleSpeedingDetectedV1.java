package com.transportlogistics.app.tracking;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Minimized durable Tracking speeding fact consumed by Notification. */
public record VehicleSpeedingDetectedV1(
        UUID eventId, UUID tenantId, OffsetDateTime occurredAt,
        UUID vehicleId, UUID driverId, UUID tripId, UUID routeId, String routeVersion,
        BigDecimal observedSpeedKph, BigDecimal effectiveThresholdKph, String thresholdSource,
        UUID ruleId, long ruleVersion, String severity, String sourceTimestamp, int repeatCount)
        implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "VEHICLE_SPEEDING_DETECTED_V1";
    public static final String CONSUMER = "speeding-episode-notification-bridge";
    private static final Set<String> THRESHOLD_SOURCES = Set.of("ROUTE_CONFIG", "TENANT_CONFIG");
    private static final Set<String> SEVERITIES = Set.of("WARNING", "HIGH");

    public VehicleSpeedingDetectedV1 {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(vehicleId, "vehicleId is required");
        Objects.requireNonNull(observedSpeedKph, "observedSpeedKph is required");
        Objects.requireNonNull(effectiveThresholdKph, "effectiveThresholdKph is required");
        Objects.requireNonNull(ruleId, "ruleId is required");
        Objects.requireNonNull(sourceTimestamp, "sourceTimestamp is required");
        if (!THRESHOLD_SOURCES.contains(thresholdSource) || !SEVERITIES.contains(severity)
                || ruleVersion < 1 || repeatCount < 0) {
            throw new IllegalArgumentException("Speeding event value is invalid");
        }
    }

    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "SPEEDING_EPISODE"; }
    @Override public UUID aggregateId() { return eventId; }
    @Override public String durableConsumer() { return CONSUMER; }

    @Override
    public Map<String, ?> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("speedEpisodeId", eventId.toString());
        payload.put("vehicleId", vehicleId.toString());
        payload.put("driverId", driverId == null ? null : driverId.toString());
        payload.put("tripId", tripId == null ? null : tripId.toString());
        payload.put("routeId", routeId == null ? null : routeId.toString());
        payload.put("routeVersion", routeVersion);
        payload.put("observedSpeedKph", observedSpeedKph);
        payload.put("effectiveThresholdKph", effectiveThresholdKph);
        payload.put("thresholdSource", thresholdSource);
        payload.put("ruleId", ruleId.toString());
        payload.put("ruleVersion", ruleVersion);
        payload.put("severity", severity);
        payload.put("sourceTimestamp", sourceTimestamp);
        payload.put("repeatCount", repeatCount);
        return Collections.unmodifiableMap(payload);
    }
}
