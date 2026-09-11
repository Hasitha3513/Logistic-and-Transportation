package com.transportlogistics.app.tracking;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Minimized durable Tracking fact consumed by Notification. */
public record VehicleGeofenceTransitionedV1(
        UUID eventId,
        UUID tenantId,
        OffsetDateTime occurredAt,
        UUID geofenceId,
        UUID vehicleId,
        UUID locationId,
        String geofenceType,
        String transition,
        String severity,
        String sourceTimestamp,
        long definitionVersion) implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "VEHICLE_GEOFENCE_TRANSITIONED_V1";
    public static final String CONSUMER = "geofence-transition-notification-bridge";
    private static final Set<String> GEOFENCE_TYPES =
            Set.of("DEPOT", "CUSTOMER_SITE", "UNAUTHORIZED_ZONE");
    private static final Set<String> TRANSITIONS =
            Set.of("ENTERED", "EXITED", "UNAUTHORIZED_ZONE_ENTERED");
    private static final Set<String> SEVERITIES = Set.of("NORMAL", "HIGH");

    public VehicleGeofenceTransitionedV1 {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(geofenceId, "geofenceId is required");
        Objects.requireNonNull(vehicleId, "vehicleId is required");
        if (!GEOFENCE_TYPES.contains(geofenceType)) {
            throw new IllegalArgumentException("Unsupported geofence type");
        }
        if (!TRANSITIONS.contains(transition)) {
            throw new IllegalArgumentException("Unsupported geofence transition");
        }
        if (!SEVERITIES.contains(severity)) {
            throw new IllegalArgumentException("Unsupported geofence severity");
        }
        Objects.requireNonNull(sourceTimestamp, "sourceTimestamp is required");
        if (definitionVersion < 1) {
            throw new IllegalArgumentException("definitionVersion must be positive");
        }
    }

    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "GEOFENCE_TRANSITION"; }
    @Override public UUID aggregateId() { return eventId; }
    @Override public String durableConsumer() { return CONSUMER; }

    @Override
    public Map<String, ?> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("geofenceId", geofenceId.toString());
        payload.put("vehicleId", vehicleId.toString());
        payload.put("locationId", locationId == null ? null : locationId.toString());
        payload.put("geofenceType", geofenceType);
        payload.put("transition", transition);
        payload.put("severity", severity);
        payload.put("sourceTimestamp", sourceTimestamp);
        payload.put("definitionVersion", definitionVersion);
        return Collections.unmodifiableMap(payload);
    }
}
