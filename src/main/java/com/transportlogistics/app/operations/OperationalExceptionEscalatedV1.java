package com.transportlogistics.app.operations;

import com.transportlogistics.app.shared.DurableEventEnvelope;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Safe durable fact consumed by Notification through the system bridge. */
public record OperationalExceptionEscalatedV1(
        UUID eventId,
        UUID tenantId,
        UUID caseId,
        String caseReference,
        String sourceModule,
        String sourceType,
        UUID sourceId,
        String category,
        String severity,
        String escalationLevel,
        String slaStatus,
        OffsetDateTime occurredAt,
        String correlationId
) implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "OPERATIONAL_EXCEPTION_ESCALATED_V1";
    public static final String CONSUMER = "operations-notification";

    public OperationalExceptionEscalatedV1 {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(caseId);
        Objects.requireNonNull(sourceId);
        Objects.requireNonNull(occurredAt);
        caseReference = required(caseReference, 16);
        sourceModule = required(sourceModule, 24);
        sourceType = required(sourceType, 80);
        category = required(category, 24);
        severity = required(severity, 16);
        escalationLevel = required(escalationLevel, 8);
        slaStatus = required(slaStatus, 16);
        correlationId = correlationId == null || correlationId.isBlank() ? null : required(correlationId, 128);
    }

    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "OPERATIONAL_EXCEPTION_CASE"; }
    @Override public UUID aggregateId() { return caseId; }
    @Override public String durableConsumer() { return CONSUMER; }

    @Override
    public Map<String, ?> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("caseReference", caseReference);
        payload.put("sourceModule", sourceModule);
        payload.put("sourceType", sourceType);
        payload.put("sourceId", sourceId.toString());
        payload.put("category", category);
        payload.put("severity", severity);
        payload.put("escalationLevel", escalationLevel);
        payload.put("slaStatus", slaStatus);
        if (correlationId != null) payload.put("correlationId", correlationId);
        return Map.copyOf(payload);
    }

    private static String required(String value, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new IllegalArgumentException("Invalid operational escalation payload");
        }
        return value.trim();
    }
}
