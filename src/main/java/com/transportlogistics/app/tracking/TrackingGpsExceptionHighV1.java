package com.transportlogistics.app.tracking;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Tracking-owned minimized HIGH episode fact, translated by the system bridge for Operations. */
public record TrackingGpsExceptionHighV1(UUID eventId, UUID tenantId, OffsetDateTime occurredAt,
        UUID episodeId, String exceptionType, String category, Map<String, String> safeMetadata)
        implements DurableEventEnvelope {
    public static final String EVENT_TYPE = "TRACKING_GPS_EXCEPTION_HIGH_V1";
    public static final String CONSUMER = "tracking-gps-exception-operations-bridge";
    public TrackingGpsExceptionHighV1 { safeMetadata = Map.copyOf(safeMetadata); }
    @Override public String eventType() { return EVENT_TYPE; }
    @Override public int version() { return 1; }
    @Override public String aggregateType() { return "TRACKING_GPS_EXCEPTION_EPISODE"; }
    @Override public UUID aggregateId() { return episodeId; }
    @Override public String durableConsumer() { return CONSUMER; }
    @Override public Map<String, ?> payload() { return Map.of("episodeId", episodeId.toString(),
            "exceptionType", exceptionType, "category", category, "safeMetadata", safeMetadata); }
}
