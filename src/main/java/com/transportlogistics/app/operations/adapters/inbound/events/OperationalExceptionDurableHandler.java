package com.transportlogistics.app.operations.adapters.inbound.events;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OperationalExceptionDurableHandler implements DurableEventHandler {
    private final OperationalExceptionUseCase operations;
    public OperationalExceptionDurableHandler(OperationalExceptionUseCase operations) { this.operations = operations; }
    @Override public String consumerName() { return OperationalExceptionFactV1.CONSUMER; }

    @Override
    @Transactional
    public void handle(DurableEventEnvelope envelope) {
        try {
            if (!OperationalExceptionFactV1.EVENT_TYPE.equals(envelope.eventType()) || envelope.version() != 1) {
                throw new IllegalArgumentException("Unsupported operational exception fact");
            }
            Map<String, ?> payload = envelope.payload();
            Map<String, String> metadata = strings(payload.get("safeMetadata"));
            operations.intake(new OperationalExceptionFactV1(envelope.eventId(), envelope.tenantId(),
                OperationalExceptionFactV1.SourceModule.valueOf(text(payload, "sourceModule")),
                text(payload, "sourceType"), UUID.fromString(text(payload, "sourceId")), envelope.occurredAt(),
                OperationalExceptionFactV1.Severity.valueOf(text(payload, "severityCandidate")),
                OperationalExceptionFactV1.Category.valueOf(text(payload, "categoryCandidate")),
                text(payload, "summaryCode"), metadata, nullableText(payload, "correlationId")));
        } catch (IllegalArgumentException | ClassCastException exception) {
            throw new PermanentEventFailureException("INVALID_OPERATIONAL_EXCEPTION_FACT", exception.getMessage());
        }
    }

    private static String text(Map<String, ?> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException(key + " is required");
        return text;
    }
    private static String nullableText(Map<String, ?> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }
    private static Map<String, String> strings(Object value) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> source)) throw new IllegalArgumentException("safeMetadata must be an object");
        Map<String, String> target = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (!(key instanceof String textKey) || !(item instanceof String textValue)) {
                throw new IllegalArgumentException("safeMetadata values must be strings");
            }
            target.put(textKey, textValue);
        });
        return Map.copyOf(target);
    }
}
