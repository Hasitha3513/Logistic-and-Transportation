package com.transportlogistics.app.tracking;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Minimized durable fact for the first opening of a GPS exception episode. */
public record TrackingGpsExceptionOpenedV1(
        UUID eventId, UUID tenantId, OffsetDateTime occurredAt, UUID episodeId,
        String exceptionTypeLabel, String severity, String vehicleLabel, String observedAt)
        implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "TRACKING_GPS_EXCEPTION_OPENED";
    public static final String CONSUMER = "tracking-gps-exception-notification";

    public TrackingGpsExceptionOpenedV1 {
        Objects.requireNonNull(eventId); Objects.requireNonNull(tenantId);
        Objects.requireNonNull(occurredAt); Objects.requireNonNull(episodeId);
        if (!eventId.equals(episodeId) || !Set.of("WARNING", "HIGH").contains(severity)
                || exceptionTypeLabel == null || exceptionTypeLabel.isBlank()
                || vehicleLabel == null || vehicleLabel.isBlank()
                || observedAt == null || observedAt.isBlank()) {
            throw new IllegalArgumentException("GPS exception notification event is invalid");
        }
    }

    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "TRACKING_GPS_EXCEPTION_EPISODE"; }
    @Override public UUID aggregateId() { return episodeId; }
    @Override public String durableConsumer() { return CONSUMER; }
    @Override public Map<String, ?> payload() {
        return Map.of("exceptionTypeLabel", exceptionTypeLabel, "severity", severity,
                "vehicleLabel", vehicleLabel, "observedAt", observedAt);
    }
}
