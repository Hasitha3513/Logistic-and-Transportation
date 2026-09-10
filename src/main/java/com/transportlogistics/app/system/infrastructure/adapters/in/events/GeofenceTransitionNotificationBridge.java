package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class GeofenceTransitionNotificationBridge implements DurableEventHandler {
    private static final Set<String> REQUIRED = Set.of(
            "geofenceId", "vehicleId", "geofenceType", "transition", "severity",
            "sourceTimestamp", "definitionVersion");
    private static final Set<String> ALLOWED = Set.of(
            "geofenceId", "vehicleId", "locationId", "geofenceType", "transition", "severity",
            "sourceTimestamp", "definitionVersion");
    private static final Set<String> GEOFENCE_TYPES =
            Set.of("DEPOT", "CUSTOMER_SITE", "UNAUTHORIZED_ZONE");
    private static final Set<String> TRANSITIONS =
            Set.of("ENTERED", "EXITED", "UNAUTHORIZED_ZONE_ENTERED");
    private static final Set<String> SEVERITIES = Set.of("NORMAL", "HIGH");
    private static final String EVENT_TYPE = "VEHICLE_GEOFENCE_TRANSITIONED_V1";
    private static final String CONSUMER = "geofence-transition-notification-bridge";
    private final OperationalNotificationPublisher notifications;

    public GeofenceTransitionNotificationBridge(OperationalNotificationPublisher notifications) {
        this.notifications = notifications;
    }

    @Override
    public String consumerName() {
        return CONSUMER;
    }

    @Override
    public void handle(DurableEventEnvelope envelope) {
        try {
            if (envelope == null || envelope.eventId() == null || envelope.tenantId() == null
                    || envelope.occurredAt() == null || envelope.aggregateId() == null
                    || envelope.payload() == null) {
                throw new IllegalArgumentException("Geofence transition envelope is incomplete");
            }
            if (!EVENT_TYPE.equals(envelope.eventType())
                    || envelope.version() != 1
                    || !"GEOFENCE_TRANSITION".equals(envelope.aggregateType())) {
                throw new IllegalArgumentException("Unsupported geofence transition event");
            }
            if (!envelope.payload().keySet().equals(ALLOWED)
                    || !envelope.payload().keySet().containsAll(REQUIRED)) {
                throw new IllegalArgumentException("Geofence transition payload fields do not match version 1");
            }
            Map<String, String> metadata = metadata(envelope.payload());
            UUID.fromString(metadata.get("geofenceId"));
            UUID.fromString(metadata.get("vehicleId"));
            if (metadata.containsKey("locationId")) UUID.fromString(metadata.get("locationId"));
            Instant.parse(metadata.get("sourceTimestamp"));
            long definitionVersion = Long.parseLong(metadata.get("definitionVersion"));
            if (definitionVersion < 1 || !GEOFENCE_TYPES.contains(metadata.get("geofenceType"))
                    || !TRANSITIONS.contains(metadata.get("transition"))
                    || !SEVERITIES.contains(metadata.get("severity"))) {
                throw new IllegalArgumentException("Geofence transition payload value is invalid");
            }
            var severity = "HIGH".equals(metadata.get("severity"))
                    ? OperationalNotificationEvent.Severity.CRITICAL
                    : OperationalNotificationEvent.Severity.INFO;
            notifications.publish(new OperationalNotificationEvent(
                    envelope.eventId(), envelope.eventType(), envelope.aggregateType(), envelope.aggregateId(),
                    severity, envelope.eventType(), envelope.eventType(), envelope.occurredAt(),
                    Map.copyOf(metadata), envelope.tenantId(), envelope.version()));
        } catch (IllegalArgumentException exception) {
            throw new PermanentEventFailureException(
                    "INVALID_GEOFENCE_TRANSITION_EVENT", exception.getMessage());
        }
    }

    private static Map<String, String> metadata(Map<String, ?> payload) {
        Map<String, String> metadata = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (value != null) metadata.put(key, String.valueOf(value));
        });
        if (!metadata.keySet().containsAll(REQUIRED)) {
            throw new IllegalArgumentException("Required geofence transition payload field is missing");
        }
        return metadata;
    }
}
