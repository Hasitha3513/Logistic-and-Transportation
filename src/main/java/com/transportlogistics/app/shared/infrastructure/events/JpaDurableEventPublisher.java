package com.transportlogistics.app.shared.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Component
public class JpaDurableEventPublisher implements DurableEventPublisher {
    static final int MAX_PAYLOAD_BYTES = 32 * 1024;
    private final IntegrationOutboxJpaRepository events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JpaDurableEventPublisher(IntegrationOutboxJpaRepository events, ObjectMapper objectMapper, Clock clock) {
        this.events = events;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void publish(DurableEventEnvelope event) {
        validate(event);
        if (events.existsByEventIdAndConsumerName(event.eventId(), event.durableConsumer())) {
            return;
        }
        try {
            String payload = objectMapper.writer()
                .with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsString(event.payload());
            if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("Durable event payload exceeds 32 KiB");
            }
            events.save(IntegrationOutboxEventEntity.pending(event, payload, now()));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Durable event payload cannot be serialized", exception);
        }
    }

    private void validate(DurableEventEnvelope event) {
        Objects.requireNonNull(event, "Durable event cannot be null");
        Objects.requireNonNull(event.eventId(), "eventId cannot be null");
        Objects.requireNonNull(event.tenantId(), "tenantId cannot be null");
        Objects.requireNonNull(event.occurredAt(), "occurredAt cannot be null");
        Objects.requireNonNull(event.aggregateId(), "aggregateId cannot be null");
        Objects.requireNonNull(event.payload(), "payload cannot be null");
        if (event.eventType() == null || event.eventType().isBlank()
                || event.aggregateType() == null || event.aggregateType().isBlank()
                || event.durableConsumer() == null || event.durableConsumer().isBlank()) {
            throw new IllegalArgumentException("Durable event routing and aggregate facts are required");
        }
        if (event.version() < 1) {
            throw new IllegalArgumentException("Durable event version must be positive");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
