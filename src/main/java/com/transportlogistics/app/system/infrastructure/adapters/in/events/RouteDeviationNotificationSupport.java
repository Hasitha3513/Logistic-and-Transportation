package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class RouteDeviationNotificationSupport {
    private static final Set<String> COMMON = Set.of("routeDeviationEpisodeId", "vehicleId", "tripId",
            "driverId", "routeId", "routeVersion", "severity", "observedDistanceMeters",
            "effectiveToleranceMeters", "sourceTimestamp", "approvalRequired");
    private RouteDeviationNotificationSupport() { }

    static OperationalNotificationEvent map(DurableEventEnvelope envelope, boolean escalation) {
        Set<String> fields = escalation ? union(COMMON, "escalationReason") : COMMON;
        if (envelope == null || envelope.eventId() == null || envelope.tenantId() == null
                || envelope.occurredAt() == null || envelope.aggregateId() == null || envelope.payload() == null
                || envelope.version() != 1 || !"ROUTE_DEVIATION_EPISODE".equals(envelope.aggregateType())
                || !envelope.payload().keySet().equals(fields)) {
            throw new IllegalArgumentException("Route-deviation envelope is invalid");
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        envelope.payload().forEach((key, value) -> { if (value != null) metadata.put(key, String.valueOf(value)); });
        UUID episodeId = UUID.fromString(required(metadata, "routeDeviationEpisodeId"));
        UUID.fromString(required(metadata, "vehicleId")); UUID.fromString(required(metadata, "routeId"));
        optionalUuid(metadata, "tripId"); optionalUuid(metadata, "driverId");
        if (!episodeId.equals(envelope.aggregateId())) throw new IllegalArgumentException("Episode identity differs");
        String severity = required(metadata, "severity");
        if (!Set.of("WARNING", "HIGH").contains(severity)) throw new IllegalArgumentException("Severity is invalid");
        boolean approval = Boolean.parseBoolean(required(metadata, "approvalRequired"));
        if (approval != "HIGH".equals(severity)) throw new IllegalArgumentException("Approval fact is invalid");
        Instant source = Instant.parse(required(metadata, "sourceTimestamp"));
        BigDecimal observed = new BigDecimal(required(metadata, "observedDistanceMeters"));
        BigDecimal tolerance = new BigDecimal(required(metadata, "effectiveToleranceMeters"));
        if (observed.signum() < 0 || tolerance.signum() <= 0) throw new IllegalArgumentException("Distance is invalid");
        metadata.put("observedDistanceMeters", observed.setScale(0, RoundingMode.HALF_UP).toPlainString());
        metadata.put("sourceTimestamp", source.toString());
        String title;
        String body;
        OperationalNotificationEvent.Severity mapped;
        if (escalation) {
            String reason = required(metadata, "escalationReason");
            if (!Set.of("DISTANCE_HIGH", "REVIEW_REJECTED").contains(reason) || !"HIGH".equals(severity)) {
                throw new IllegalArgumentException("Escalation is invalid");
            }
            mapped = OperationalNotificationEvent.Severity.CRITICAL;
            title = "Route deviation escalated — " + reason;
            body = "Route deviation episode " + episodeId + " for vehicle " + metadata.get("vehicleId")
                    + " requires immediate attention. Route " + metadata.get("routeId") + " ("
                    + metadata.get("routeVersion") + "); observed distance " + metadata.get("observedDistanceMeters")
                    + " m at " + metadata.get("sourceTimestamp") + ". Reason: " + reason + ".";
        } else {
            mapped = "HIGH".equals(severity) ? OperationalNotificationEvent.Severity.CRITICAL
                    : OperationalNotificationEvent.Severity.WARNING;
            metadata.put("domainSeverity", severity);
            metadata.put("highSuffix", "HIGH".equals(severity) ? " Approval is required." : "");
            title = "Route deviation detected — " + severity;
            body = "Vehicle " + metadata.get("vehicleId") + " deviated from route " + metadata.get("routeId")
                    + " (" + metadata.get("routeVersion") + ") by " + metadata.get("observedDistanceMeters")
                    + " m at " + metadata.get("sourceTimestamp") + ". Review episode " + episodeId + "."
                    + metadata.get("highSuffix");
        }
        return new OperationalNotificationEvent(envelope.eventId(), envelope.eventType(), envelope.aggregateType(),
                envelope.aggregateId(), mapped, title, body, envelope.occurredAt(), Map.copyOf(metadata),
                envelope.tenantId(), 1);
    }
    private static String required(Map<String, String> values, String key) {
        String value = values.get(key); if (value == null || value.isBlank()) throw new IllegalArgumentException(key);
        return value;
    }
    private static void optionalUuid(Map<String, String> values, String key) {
        if (values.containsKey(key)) UUID.fromString(values.get(key));
    }
    private static Set<String> union(Set<String> source, String value) {
        var result = new java.util.HashSet<>(source); result.add(value); return Set.copyOf(result);
    }
}
