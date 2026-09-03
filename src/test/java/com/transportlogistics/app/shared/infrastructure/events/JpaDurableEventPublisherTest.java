package com.transportlogistics.app.shared.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaDurableEventPublisherTest {
    private final IntegrationOutboxJpaRepository repository = mock(IntegrationOutboxJpaRepository.class);
    private final JpaDurableEventPublisher publisher = new JpaDurableEventPublisher(repository, new ObjectMapper(),
        Clock.fixed(Instant.parse("2026-09-03T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void storesOneStableLogicalEventAndDeduplicatesReplay() {
        var event = event();

        publisher.publish(event);
        verify(repository).save(any(IntegrationOutboxEventEntity.class));

        when(repository.existsByEventIdAndConsumerName(event.eventId(), event.durableConsumer())).thenReturn(true);
        publisher.publish(event);
        verify(repository).save(any(IntegrationOutboxEventEntity.class));
    }

    @Test
    void duplicateKnownBeforeSerializationDoesNotWrite() {
        var event = event();
        when(repository.existsByEventIdAndConsumerName(event.eventId(), event.durableConsumer())).thenReturn(true);

        publisher.publish(event);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsAnOversizedPayloadBeforePersistence() {
        DurableEventEnvelope event = new DurableEventEnvelope() {
            @Override
            public UUID eventId() { return UUID.randomUUID(); }
            @Override
            public String eventType() { return "OVERSIZED"; }
            @Override
            public UUID tenantId() { return UUID.randomUUID(); }
            @Override
            public OffsetDateTime occurredAt() { return OffsetDateTime.parse("2026-09-03T09:59:00Z"); }
            @Override
            public int version() { return 1; }
            @Override
            public String aggregateType() { return "TEST"; }
            @Override
            public UUID aggregateId() { return UUID.randomUUID(); }
            @Override
            public Map<String, ?> payload() {
                return Map.of("data", "x".repeat(JpaDurableEventPublisher.MAX_PAYLOAD_BYTES));
            }
            @Override
            public String durableConsumer() { return "consumer"; }
        };

        assertThatThrownBy(() -> publisher.publish(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("32 KiB");
        verify(repository, never()).save(any());
    }

    private DeliveryCustomerNotificationEvent event() {
        return new DeliveryCustomerNotificationEvent(UUID.randomUUID(), "DELIVERY_COMPLETED", UUID.randomUUID(),
            OffsetDateTime.parse("2026-09-03T09:59:00Z"), 1, "DELIVERY_ORDER", UUID.randomUUID(),
            Map.of("customerId", UUID.randomUUID().toString(), "deliveryNumber", "DEL-1", "actor", "system",
                "status", "DELIVERED", "completedAt", "2026-09-03T09:58:00Z"));
    }
}
