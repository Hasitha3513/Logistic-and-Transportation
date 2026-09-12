package com.transportlogistics.app.tracking.adapters.outbound.events;

import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.tracking.ports.outbound.SpeedingEpisodePublisherPort;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class DurableSpeedingEpisodePublisher implements SpeedingEpisodePublisherPort {
    private final DurableEventPublisher events;

    DurableSpeedingEpisodePublisher(DurableEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(UUID tenantId, SpeedingEpisodePublisherPort.VehicleSpeedingDetectedV1 event) {
        events.publish(new com.transportlogistics.app.tracking.VehicleSpeedingDetectedV1(
                event.speedEpisodeId(), tenantId,
                OffsetDateTime.ofInstant(event.sourceTimestamp(), ZoneOffset.UTC), event.vehicleId(),
                event.driverId(), event.tripId(), event.routeId(), event.routeVersion(),
                event.observedSpeedKph().value(), event.effectiveThresholdKph().value(),
                event.thresholdSource().name(), event.ruleId(), event.ruleVersion(),
                event.severity().name(), event.sourceTimestamp().toString(), event.repeatCount()));
    }
}
