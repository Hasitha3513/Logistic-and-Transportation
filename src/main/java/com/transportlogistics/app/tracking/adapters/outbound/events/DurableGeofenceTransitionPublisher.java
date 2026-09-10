package com.transportlogistics.app.tracking.adapters.outbound.events;

import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tracking.ports.outbound.GeofenceTransitionPublisherPort;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
final class DurableGeofenceTransitionPublisher implements GeofenceTransitionPublisherPort {
    private final DurableEventPublisher events;

    DurableGeofenceTransitionPublisher(DurableEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(GeofenceTransitionPublisherPort.VehicleGeofenceTransitionedV1 event) {
        events.publish(new com.transportlogistics.app.tracking.VehicleGeofenceTransitionedV1(
                event.eventId(), event.tenantId(),
                OffsetDateTime.ofInstant(event.sourceTimestamp(), ZoneOffset.UTC),
                event.geofenceId(), event.vehicleId(), event.locationId(),
                event.geofenceType().name(), event.transition().name(), event.severity().name(),
                event.sourceTimestamp().toString(), event.definitionVersion()));
    }
}
