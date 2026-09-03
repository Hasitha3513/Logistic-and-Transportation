package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeliveryCustomerNotificationEventBridgeTest {
    @Test
    void durableReplayPreservesIdentityTenantVersionAndFrozenPayload() {
        OperationalNotificationPublisher publisher = mock(OperationalNotificationPublisher.class);
        var bridge = new DeliveryCustomerNotificationEventBridge(publisher);
        var source = new DeliveryCustomerNotificationEvent(UUID.randomUUID(), "DELIVERY_COMPLETED",
            UUID.randomUUID(), OffsetDateTime.parse("2026-09-03T10:00:00Z"), 1, "DELIVERY_ORDER",
            UUID.randomUUID(), Map.of("customerId", UUID.randomUUID().toString(), "deliveryNumber", "DEL-1",
                "actor", "system", "status", "DELIVERED", "completedAt", "2026-09-03T10:00:00Z"));

        bridge.handle(source);

        ArgumentCaptor<OperationalNotificationEvent> captured = ArgumentCaptor.forClass(
            OperationalNotificationEvent.class);
        verify(publisher).publish(captured.capture());
        assertThat(captured.getValue().eventId()).isEqualTo(source.eventId());
        assertThat(captured.getValue().tenantId()).isEqualTo(source.tenantId());
        assertThat(captured.getValue().version()).isEqualTo(1);
        assertThat(captured.getValue().metadata()).isEqualTo(source.payload());
    }
}
