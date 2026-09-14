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

/** Minimized durable Tracking route-deviation confirmation fact. */
public record VehicleRouteDeviationDetectedV1(
        UUID eventId, UUID tenantId, OffsetDateTime occurredAt,
        UUID routeDeviationEpisodeId, UUID vehicleId, UUID tripId, UUID driverId,
        UUID routeId, String routeVersion, String severity,
        BigDecimal observedDistanceMeters, BigDecimal effectiveToleranceMeters,
        String sourceTimestamp, boolean approvalRequired) implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "VEHICLE_ROUTE_DEVIATION_DETECTED_V1";
    public static final String CONSUMER = "route-deviation-detected-notification-bridge";
    private static final Set<String> SEVERITIES = Set.of("WARNING", "HIGH");

    public VehicleRouteDeviationDetectedV1 {
        Objects.requireNonNull(eventId); Objects.requireNonNull(tenantId); Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(routeDeviationEpisodeId); Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(routeId); Objects.requireNonNull(routeVersion); Objects.requireNonNull(severity);
        Objects.requireNonNull(observedDistanceMeters); Objects.requireNonNull(effectiveToleranceMeters);
        Objects.requireNonNull(sourceTimestamp);
        if (!eventId.equals(routeDeviationEpisodeId) || !SEVERITIES.contains(severity)
                || observedDistanceMeters.signum() < 0 || effectiveToleranceMeters.signum() <= 0
                || approvalRequired != "HIGH".equals(severity)) {
            throw new IllegalArgumentException("Route-deviation detection event is invalid");
        }
    }

    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "ROUTE_DEVIATION_EPISODE"; }
    @Override public UUID aggregateId() { return routeDeviationEpisodeId; }
    @Override public String durableConsumer() { return CONSUMER; }
    @Override public Map<String, ?> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routeDeviationEpisodeId", routeDeviationEpisodeId.toString());
        payload.put("vehicleId", vehicleId.toString());
        payload.put("tripId", tripId == null ? null : tripId.toString());
        payload.put("driverId", driverId == null ? null : driverId.toString());
        payload.put("routeId", routeId.toString()); payload.put("routeVersion", routeVersion);
        payload.put("severity", severity); payload.put("observedDistanceMeters", observedDistanceMeters);
        payload.put("effectiveToleranceMeters", effectiveToleranceMeters);
        payload.put("sourceTimestamp", sourceTimestamp); payload.put("approvalRequired", approvalRequired);
        return Collections.unmodifiableMap(payload);
    }
}
