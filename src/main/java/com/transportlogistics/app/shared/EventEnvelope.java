package com.transportlogistics.app.shared;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Canonical versioned event envelope shared by active internal consumers. */
public interface EventEnvelope {
    UUID eventId();

    String eventType();

    UUID tenantId();

    OffsetDateTime occurredAt();

    int version();

    String aggregateType();

    UUID aggregateId();

    Map<String, ?> payload();
}
