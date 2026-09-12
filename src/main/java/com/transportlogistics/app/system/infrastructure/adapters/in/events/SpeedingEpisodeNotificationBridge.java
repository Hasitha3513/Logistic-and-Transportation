package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.tracking.VehicleSpeedingDetectedV1;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class SpeedingEpisodeNotificationBridge implements DurableEventHandler {
    private static final Set<String> FIELDS = Set.of(
            "speedEpisodeId", "vehicleId", "driverId", "tripId", "routeId", "routeVersion",
            "observedSpeedKph", "effectiveThresholdKph", "thresholdSource", "ruleId",
            "ruleVersion", "severity", "sourceTimestamp", "repeatCount");
    private final OperationalNotificationPublisher notifications;

    public SpeedingEpisodeNotificationBridge(OperationalNotificationPublisher notifications) {
        this.notifications = notifications;
    }

    @Override public String consumerName() { return VehicleSpeedingDetectedV1.CONSUMER; }

    @Override
    public void handle(DurableEventEnvelope envelope) {
        try {
            validateEnvelope(envelope);
            Map<String, String> metadata = metadata(envelope.payload());
            validateValues(envelope, metadata);
            var notificationSeverity = "HIGH".equals(metadata.get("severity"))
                    ? OperationalNotificationEvent.Severity.CRITICAL
                    : OperationalNotificationEvent.Severity.WARNING;
            notifications.publish(new OperationalNotificationEvent(
                    envelope.eventId(), envelope.eventType(), envelope.aggregateType(), envelope.aggregateId(),
                    notificationSeverity, envelope.eventType(), envelope.eventType(), envelope.occurredAt(),
                    Map.copyOf(metadata), envelope.tenantId(), envelope.version()));
        } catch (IllegalArgumentException exception) {
            throw new PermanentEventFailureException("INVALID_SPEEDING_EPISODE_EVENT", exception.getMessage());
        }
    }

    private static void validateEnvelope(DurableEventEnvelope envelope) {
        if (envelope == null || envelope.eventId() == null || envelope.tenantId() == null
                || envelope.occurredAt() == null || envelope.aggregateId() == null
                || envelope.payload() == null) {
            throw new IllegalArgumentException("Speeding episode envelope is incomplete");
        }
        if (!VehicleSpeedingDetectedV1.EVENT_TYPE.equals(envelope.eventType())
                || envelope.version() != 1 || !"SPEEDING_EPISODE".equals(envelope.aggregateType())
                || !envelope.eventId().equals(envelope.aggregateId())) {
            throw new IllegalArgumentException("Unsupported speeding episode event");
        }
        if (!envelope.payload().keySet().equals(FIELDS)) {
            throw new IllegalArgumentException("Speeding episode payload fields do not match version 1");
        }
    }

    private static void validateValues(DurableEventEnvelope envelope, Map<String, String> metadata) {
        if (!envelope.eventId().equals(UUID.fromString(metadata.get("speedEpisodeId")))) {
            throw new IllegalArgumentException("Speeding episode identity is inconsistent");
        }
        UUID.fromString(metadata.get("vehicleId"));
        UUID.fromString(metadata.get("ruleId"));
        optionalUuid(metadata, "driverId");
        optionalUuid(metadata, "tripId");
        optionalUuid(metadata, "routeId");
        Instant sourceTimestamp = Instant.parse(metadata.get("sourceTimestamp"));
        if (!envelope.occurredAt().toInstant().equals(sourceTimestamp)
                || new BigDecimal(metadata.get("observedSpeedKph")).signum() < 0
                || new BigDecimal(metadata.get("effectiveThresholdKph")).signum() <= 0
                || Long.parseLong(metadata.get("ruleVersion")) < 1
                || Integer.parseInt(metadata.get("repeatCount")) < 0
                || !Set.of("ROUTE_CONFIG", "TENANT_CONFIG").contains(metadata.get("thresholdSource"))
                || !Set.of("WARNING", "HIGH").contains(metadata.get("severity"))) {
            throw new IllegalArgumentException("Speeding episode payload value is invalid");
        }
    }

    private static void optionalUuid(Map<String, String> metadata, String field) {
        if (metadata.containsKey(field)) UUID.fromString(metadata.get(field));
    }

    private static Map<String, String> metadata(Map<String, ?> payload) {
        Map<String, String> metadata = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (value != null) metadata.put(key, String.valueOf(value));
        });
        for (String required : Set.of("speedEpisodeId", "vehicleId", "observedSpeedKph",
                "effectiveThresholdKph", "thresholdSource", "ruleId", "ruleVersion", "severity",
                "sourceTimestamp", "repeatCount")) {
            if (!metadata.containsKey(required)) {
                throw new IllegalArgumentException("Required speeding episode payload field is missing");
            }
        }
        return metadata;
    }
}
