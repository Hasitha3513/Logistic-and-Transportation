package com.transportlogistics.app.shared.infrastructure.events;

import com.transportlogistics.app.shared.DurableEventEnvelope;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

record StoredDurableEvent(
    UUID eventId,
    String eventType,
    UUID tenantId,
    OffsetDateTime occurredAt,
    int version,
    String aggregateType,
    UUID aggregateId,
    Map<String, ?> payload,
    String durableConsumer
) implements DurableEventEnvelope {}
