package com.transportlogistics.app.notification;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalNotificationEventTest {
    @Test
    void carriesStableAggregateAndTenantIdentityWithSchemaVersion() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-09-02T10:00:00Z");
        var event = new OperationalNotificationEvent(eventId, "TRIP_COMPLETED", "TRIP", aggregateId,
                OperationalNotificationEvent.Severity.INFO, "Completed", "Trip completed", occurredAt,
                Map.of("tripId", aggregateId.toString())).withTenantId(tenantId);

        assertEquals(eventId, event.eventId());
        assertEquals("TRIP_COMPLETED", event.eventType());
        assertEquals("TRIP", event.aggregateType());
        assertEquals(aggregateId, event.aggregateId());
        assertEquals(tenantId, event.tenantId());
        assertEquals(occurredAt, event.occurredAt());
        assertEquals(1, event.schemaVersion());
    }
}
