package com.transportlogistics.app.tracking.adapters.outbound.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceSeverity;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceTransitionType;
import com.transportlogistics.app.tracking.domain.geofence.GeofenceType;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionPublisherPort;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DurableGeofenceTransitionPublisherTest {
    @Test
    void mapsThePortEventToTheCanonicalDurableEnvelope() {
        DurableEventPublisher durable = mock(DurableEventPublisher.class);
        var adapter = new DurableGeofenceTransitionPublisher(durable);
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant source = Instant.parse("2026-09-11T01:00:00Z");

        adapter.publish(new GeofenceTransitionPublisherPort.VehicleGeofenceTransitionedV1(
                eventId, tenantId, UUID.randomUUID(), UUID.randomUUID(), null,
                GeofenceType.UNAUTHORIZED_ZONE, GeofenceTransitionType.UNAUTHORIZED_ZONE_ENTERED,
                GeofenceSeverity.HIGH, source, 2));

        var envelope = forClass(DurableEventEnvelope.class);
        verify(durable).publish(envelope.capture());
        assertThat(envelope.getValue().eventId()).isEqualTo(eventId);
        assertThat(envelope.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(envelope.getValue().occurredAt().toInstant()).isEqualTo(source);
        assertThat(envelope.getValue().payload().get("severity")).isEqualTo("HIGH");
    }
}
