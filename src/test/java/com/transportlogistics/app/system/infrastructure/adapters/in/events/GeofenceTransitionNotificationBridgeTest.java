package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.tracking.VehicleGeofenceTransitionedV1;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeofenceTransitionNotificationBridgeTest {
    private final OperationalNotificationPublisher notifications = mock(OperationalNotificationPublisher.class);
    private final GeofenceTransitionNotificationBridge bridge = new GeofenceTransitionNotificationBridge(notifications);

    @Test
    void validatesAndMapsTheMinimizedEnvelopeWithoutATrackingLookup() {
        UUID tenantId = UUID.randomUUID();
        var event = new VehicleGeofenceTransitionedV1(UUID.randomUUID(), tenantId,
                OffsetDateTime.parse("2026-09-11T01:00:00Z"), UUID.randomUUID(), UUID.randomUUID(), null,
                "UNAUTHORIZED_ZONE", "UNAUTHORIZED_ZONE_ENTERED", "HIGH",
                "2026-09-11T01:00:00Z", 1);

        bridge.handle(event);

        var captured = forClass(OperationalNotificationEvent.class);
        verify(notifications).publish(captured.capture());
        assertThat(captured.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(captured.getValue().severity()).isEqualTo(OperationalNotificationEvent.Severity.CRITICAL);
        assertThat(captured.getValue().metadata()).doesNotContainKeys(
                "latitude", "longitude", "deviceId", "providerKeyId", "driverId", "customerId");
    }

    @Test
    void permanentlyRejectsUnsupportedVersionAndMalformedPayload() {
        assertThatThrownBy(() -> bridge.handle(envelope(2, Map.of())))
                .isInstanceOf(PermanentEventFailureException.class);
        assertThatThrownBy(() -> bridge.handle(envelope(1, Map.of("latitude", 1))))
                .isInstanceOf(PermanentEventFailureException.class);
    }

    private static DurableEventEnvelope envelope(int version, Map<String, ?> payload) {
        return new DurableEventEnvelope() {
            public UUID eventId() { return UUID.randomUUID(); }
            public String eventType() { return VehicleGeofenceTransitionedV1.EVENT_TYPE; }
            public UUID tenantId() { return UUID.randomUUID(); }
            public OffsetDateTime occurredAt() { return OffsetDateTime.now(); }
            public int version() { return version; }
            public String aggregateType() { return "GEOFENCE_TRANSITION"; }
            public UUID aggregateId() { return UUID.randomUUID(); }
            public Map<String, ?> payload() { return payload; }
            public String durableConsumer() { return VehicleGeofenceTransitionedV1.CONSUMER; }
        };
    }
}
