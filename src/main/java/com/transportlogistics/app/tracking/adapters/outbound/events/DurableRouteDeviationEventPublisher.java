package com.transportlogistics.app.tracking.adapters.outbound.events;

import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tracking.VehicleRouteDeviationDetectedV1;
import com.transportlogistics.app.tracking.VehicleRouteDeviationEscalatedV1;
import com.transportlogistics.app.tracking.ports.outbound.RouteDeviationEventPublisherPort;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
final class DurableRouteDeviationEventPublisher implements RouteDeviationEventPublisherPort {
    private final DurableEventPublisher events;
    DurableRouteDeviationEventPublisher(DurableEventPublisher events) { this.events = events; }

    @Override public void publishDetected(java.util.UUID tenantId, Detected event) {
        events.publish(new VehicleRouteDeviationDetectedV1(event.episodeId(), tenantId,
                OffsetDateTime.ofInstant(event.sourceTimestamp(), ZoneOffset.UTC), event.episodeId(),
                event.vehicleId(), event.tripId(), event.driverId(), event.routeId(), event.routeVersion(),
                event.severity(), event.observedDistanceMeters(), event.effectiveToleranceMeters(),
                event.sourceTimestamp().toString(), event.approvalRequired()));
    }

    @Override public void publishEscalated(java.util.UUID tenantId, Escalated event) {
        events.publish(new VehicleRouteDeviationEscalatedV1(event.eventId(), tenantId,
                OffsetDateTime.ofInstant(event.sourceTimestamp(), ZoneOffset.UTC), event.episodeId(),
                event.vehicleId(), event.tripId(), event.driverId(), event.routeId(), event.routeVersion(),
                event.severity(), event.observedDistanceMeters(), event.effectiveToleranceMeters(),
                event.sourceTimestamp().toString(), event.approvalRequired(), event.escalationReason()));
    }
}
