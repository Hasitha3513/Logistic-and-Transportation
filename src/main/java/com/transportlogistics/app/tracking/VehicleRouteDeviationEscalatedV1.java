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

/** Minimized durable Tracking route-deviation escalation fact. */
public record VehicleRouteDeviationEscalatedV1(
        UUID eventId, UUID tenantId, OffsetDateTime occurredAt,
        UUID routeDeviationEpisodeId, UUID vehicleId, UUID tripId, UUID driverId,
        UUID routeId, String routeVersion, String severity,
        BigDecimal observedDistanceMeters, BigDecimal effectiveToleranceMeters,
        String sourceTimestamp, boolean approvalRequired, String escalationReason)
        implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "VEHICLE_ROUTE_DEVIATION_ESCALATED_V1";
    public static final String CONSUMER = "route-deviation-escalated-notification-bridge";
    private static final Set<String> REASONS = Set.of("DISTANCE_HIGH", "REVIEW_REJECTED");

    public VehicleRouteDeviationEscalatedV1 {
        Objects.requireNonNull(eventId); Objects.requireNonNull(tenantId); Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(routeDeviationEpisodeId); Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(routeId); Objects.requireNonNull(routeVersion); Objects.requireNonNull(severity);
        Objects.requireNonNull(observedDistanceMeters); Objects.requireNonNull(effectiveToleranceMeters);
        Objects.requireNonNull(sourceTimestamp); Objects.requireNonNull(escalationReason);
        if (!"HIGH".equals(severity) || !approvalRequired || !REASONS.contains(escalationReason)
                || observedDistanceMeters.signum() < 0 || effectiveToleranceMeters.signum() <= 0) {
            throw new IllegalArgumentException("Route-deviation escalation event is invalid");
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
        payload.put("vehicleId", vehicleId.toString()); payload.put("tripId", tripId == null ? null : tripId.toString());
        payload.put("driverId", driverId == null ? null : driverId.toString()); payload.put("routeId", routeId.toString());
        payload.put("routeVersion", routeVersion); payload.put("severity", severity);
        payload.put("observedDistanceMeters", observedDistanceMeters);
        payload.put("effectiveToleranceMeters", effectiveToleranceMeters);
        payload.put("sourceTimestamp", sourceTimestamp); payload.put("approvalRequired", approvalRequired);
        payload.put("escalationReason", escalationReason);
        return Collections.unmodifiableMap(payload);
    }
}
