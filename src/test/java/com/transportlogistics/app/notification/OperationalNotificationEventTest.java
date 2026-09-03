package com.transportlogistics.app.notification;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertEquals(1, event.version());
        assertThat(event.payload()).containsKeys("severity", "title", "message", "metadata");
    }

    @Test
    void rejectsUnsupportedVersionsAndKeepsSecretsOutOfTheCanonicalPayload() {
        UUID tenantId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        var event = new OperationalNotificationEvent(UUID.randomUUID(), "DELIVERY_COMPLETED", "DELIVERY_ORDER",
            aggregateId, OperationalNotificationEvent.Severity.INFO, "Completed", "Delivery completed",
            OffsetDateTime.parse("2026-09-03T10:00:00Z"), Map.of("deliveryNumber", "DEL-100"), tenantId, 1);

        assertThat(event.payload().toString().toLowerCase())
            .doesNotContain("password", "jwt", "refresh token", "access_token", "magic link", "access code",
                "provider credential", "signature", "photo", "medical");
        assertThatThrownBy(() -> new OperationalNotificationEvent(UUID.randomUUID(), "DELIVERY_COMPLETED",
            "DELIVERY_ORDER", aggregateId, OperationalNotificationEvent.Severity.INFO, "Completed",
            "Delivery completed", OffsetDateTime.now(), Map.of(), tenantId, 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("version must be 1");
    }
}
