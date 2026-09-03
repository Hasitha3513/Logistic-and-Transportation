package com.transportlogistics.app.operations;

import com.transportlogistics.app.shared.DurableEventEnvelope;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Public, minimized, durable intake contract owned by Operations. */
public record OperationalExceptionFactV1(
        UUID eventId,
        UUID tenantId,
        SourceModule sourceModule,
        String sourceType,
        UUID sourceId,
        OffsetDateTime occurredAt,
        Severity severityCandidate,
        Category categoryCandidate,
        String summaryCode,
        Map<String, String> safeMetadata,
        String correlationId
) implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "OPERATIONAL_EXCEPTION_FACT_V1";
    public static final String CONSUMER = "operations-exception-intake";

    private static final Map<SourceModule, Set<String>> SOURCE_TYPES = Map.of(
        SourceModule.ROUTING, Set.of("ROAD_CLOSURE", "ACCIDENT", "WEATHER", "RESTRICTION"),
        SourceModule.DELIVERY, Set.of("DAMAGED_DELIVERY", "WRONG_ADDRESS", "PARTIAL_DELIVERY",
            "OTP_MISMATCH", "RECIPIENT_REFUSAL")
    );
    private static final Map<SourceModule, String> SUMMARY_CODES = Map.of(
        SourceModule.ROUTING, "ROUTE_DISRUPTION_CREATED",
        SourceModule.DELIVERY, "DELIVERY_EXCEPTION_CREATED"
    );
    private static final Map<SourceModule, Set<String>> METADATA_KEYS = Map.of(
        SourceModule.ROUTING, Set.of("routeId", "detourRouteId", "effectiveFrom", "effectiveUntil"),
        SourceModule.DELIVERY, Set.of("deliveryOrderId", "deliveryAttemptId")
    );

    public OperationalExceptionFactV1 {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(sourceModule, "sourceModule is required");
        Objects.requireNonNull(sourceId, "sourceId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(severityCandidate, "severityCandidate is required");
        Objects.requireNonNull(categoryCandidate, "categoryCandidate is required");
        sourceType = required(sourceType, 80, "sourceType");
        summaryCode = required(summaryCode, 80, "summaryCode");
        correlationId = optional(correlationId, 128, "correlationId");
        if (!SOURCE_TYPES.get(sourceModule).contains(sourceType)) {
            throw new IllegalArgumentException("Unregistered source type for " + sourceModule);
        }
        if (!SUMMARY_CODES.get(sourceModule).equals(summaryCode)) {
            throw new IllegalArgumentException("Unregistered summary code for " + sourceModule);
        }
        safeMetadata = safeMetadata == null ? Map.of() : Map.copyOf(safeMetadata);
        if (safeMetadata.size() > 20) {
            throw new IllegalArgumentException("safeMetadata cannot exceed 20 entries");
        }
        Set<String> allowed = METADATA_KEYS.get(sourceModule);
        int canonicalBytes = 2;
        for (var entry : safeMetadata.entrySet()) {
            String key = required(entry.getKey(), 64, "metadata key");
            String value = required(entry.getValue(), 256, "metadata value");
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unregistered safe metadata key for " + sourceModule);
            }
            canonicalBytes += key.getBytes(StandardCharsets.UTF_8).length
                + value.getBytes(StandardCharsets.UTF_8).length + 6;
        }
        if (canonicalBytes > 4096) {
            throw new IllegalArgumentException("safeMetadata canonical payload exceeds 4 KiB");
        }
    }

    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return sourceModule + "_EXCEPTION"; }
    @Override public UUID aggregateId() { return sourceId; }
    @Override public String durableConsumer() { return CONSUMER; }

    @Override
    public Map<String, ?> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceModule", sourceModule.name());
        payload.put("sourceType", sourceType);
        payload.put("sourceId", sourceId.toString());
        payload.put("severityCandidate", severityCandidate.name());
        payload.put("categoryCandidate", categoryCandidate.name());
        payload.put("summaryCode", summaryCode);
        payload.put("safeMetadata", safeMetadata);
        if (correlationId != null) payload.put("correlationId", correlationId);
        return Map.copyOf(payload);
    }

    private static String required(String value, int max, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        return trimmed;
    }

    private static String optional(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        return required(value, max, field);
    }

    public enum SourceModule { ROUTING, DELIVERY }
    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Category { OPERATIONAL, SAFETY, COMPLIANCE, CUSTOMER, FINANCIAL, TECHNICAL, SECURITY }
}
