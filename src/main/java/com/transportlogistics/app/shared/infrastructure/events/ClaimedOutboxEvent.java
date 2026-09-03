package com.transportlogistics.app.shared.infrastructure.events;

import java.time.OffsetDateTime;
import java.util.UUID;

record ClaimedOutboxEvent(
    UUID id,
    UUID eventId,
    UUID tenantId,
    String consumerName,
    String eventType,
    int eventVersion,
    String aggregateType,
    UUID aggregateId,
    String payload,
    OffsetDateTime occurredAt,
    int attemptCount
) {}
