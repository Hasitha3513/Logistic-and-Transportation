package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.operations.OperationalExceptionFactV1;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.DurableEventPublisher;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.tracking.TrackingGpsExceptionHighV1;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class GpsExceptionOperationsBridge implements DurableEventHandler {
    private final DurableEventPublisher events;
    public GpsExceptionOperationsBridge(DurableEventPublisher events) { this.events = events; }
    @Override public String consumerName() { return TrackingGpsExceptionHighV1.CONSUMER; }
    @Override public void handle(DurableEventEnvelope event) {
        try {
            if (event == null || !TrackingGpsExceptionHighV1.EVENT_TYPE.equals(event.eventType())
                    || event.version() != 1) throw new IllegalArgumentException("Unsupported GPS HIGH event");
            Map<String, ?> payload = event.payload();
            UUID episodeId = UUID.fromString(required(payload, "episodeId"));
            Map<String, String> metadata = new LinkedHashMap<>();
            Object safeMetadata = payload.get("safeMetadata");
            if (!(safeMetadata instanceof Map<?, ?> source)) {
                throw new IllegalArgumentException("safeMetadata is required");
            }
            source.forEach((key, value) -> metadata.put(String.valueOf(key), String.valueOf(value)));
            events.publish(new OperationalExceptionFactV1(event.eventId(), event.tenantId(),
                    OperationalExceptionFactV1.SourceModule.TRACKING, required(payload, "exceptionType"),
                    episodeId, event.occurredAt(), OperationalExceptionFactV1.Severity.HIGH,
                    OperationalExceptionFactV1.Category.valueOf(required(payload, "category")),
                    "TRACKING_GPS_EXCEPTION_HIGH", Map.copyOf(metadata), null));
        } catch (IllegalArgumentException | ClassCastException exception) {
            throw new PermanentEventFailureException("INVALID_GPS_EXCEPTION_HIGH_EVENT", exception.getMessage());
        }
    }

    private static String required(Map<String, ?> payload, String field) {
        Object value = payload.get(field);
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value.toString();
    }
}
