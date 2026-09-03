package com.transportlogistics.app.delivery;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryCustomerNotificationEventTest {
    @Test
    void preservesFrozenEnvelopeAndReplayIdentity() {
        UUID tenantId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        var event = DeliveryCustomerNotificationEvent.create("DELIVERY_COMPLETED", tenantId, deliveryId,
            OffsetDateTime.parse("2026-09-02T12:00:00Z"), Map.of(
                "customerId", UUID.randomUUID().toString(), "deliveryNumber", "DEL-2026-000001",
                "status", "DELIVERED", "completedAt", "2026-09-02T12:00:00Z", "actor", "operator"));

        assertThat(event.eventId()).isNotNull();
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.aggregateType()).isEqualTo("DELIVERY_ORDER");
        assertThat(event.aggregateId()).isEqualTo(deliveryId);
        assertThat(event.payload()).containsEntry("actor", "operator");
        assertThat(new DeliveryCustomerNotificationEvent(event.eventId(), event.eventType(), event.tenantId(),
            event.occurredAt(), event.version(), event.aggregateType(), event.aggregateId(), event.payload()))
            .isEqualTo(event);
        assertThat(event.durableConsumer()).isEqualTo(DeliveryCustomerNotificationEvent.DURABLE_CONSUMER);
        assertThat(event.payload().toString().toLowerCase())
            .doesNotContain("password", "jwt", "refresh token", "access_token", "magic link", "access code",
                "provider credential", "signature", "photo", "medical");
    }

    @Test
    void rejectsEventsOutsideFrozenCatalogue() {
        assertThatThrownBy(() -> DeliveryCustomerNotificationEvent.create("DELIVERY_EXTRA", UUID.randomUUID(),
            UUID.randomUUID(), OffsetDateTime.now(), Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingOrAdditionalPayloadFields() {
        assertThatThrownBy(() -> DeliveryCustomerNotificationEvent.create("DELIVERY_COMPLETED", UUID.randomUUID(),
            UUID.randomUUID(), OffsetDateTime.now(), Map.of("status", "DELIVERED")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DeliveryCustomerNotificationEvent.create("DELIVERY_OUT_FOR_DELIVERY",
            UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), Map.of(
                "customerId", UUID.randomUUID().toString(), "deliveryNumber", "DEL-2026-000001",
                "status", "OUT_FOR_DELIVERY", "actor", "operator", "phone", "+947700000001")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
