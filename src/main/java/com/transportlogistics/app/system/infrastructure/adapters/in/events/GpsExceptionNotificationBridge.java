package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.tracking.TrackingGpsExceptionOpenedV1;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class GpsExceptionNotificationBridge implements DurableEventHandler {
    private static final Set<String> FIELDS = Set.of("exceptionTypeLabel", "severity", "vehicleLabel", "observedAt");
    private final OperationalNotificationPublisher notifications;
    public GpsExceptionNotificationBridge(OperationalNotificationPublisher notifications) { this.notifications = notifications; }
    @Override public String consumerName() { return TrackingGpsExceptionOpenedV1.CONSUMER; }
    @Override public void handle(DurableEventEnvelope event) {
        try {
            if (event == null || !TrackingGpsExceptionOpenedV1.EVENT_TYPE.equals(event.eventType())
                    || event.version() != 1 || !"TRACKING_GPS_EXCEPTION_EPISODE".equals(event.aggregateType())
                    || !event.eventId().equals(event.aggregateId()) || !event.payload().keySet().equals(FIELDS)) {
                throw new IllegalArgumentException("Unsupported GPS exception notification event");
            }
            Map<String, String> metadata = new LinkedHashMap<>();
            event.payload().forEach((key, value) -> metadata.put(key, required(value, key)));
            String severity = metadata.get("severity");
            if (!Set.of("WARNING", "HIGH").contains(severity)) throw new IllegalArgumentException("Invalid severity");
            Instant.parse(metadata.get("observedAt"));
            notifications.publish(new OperationalNotificationEvent(event.eventId(), event.eventType(),
                    event.aggregateType(), event.aggregateId(), "HIGH".equals(severity)
                            ? OperationalNotificationEvent.Severity.CRITICAL
                            : OperationalNotificationEvent.Severity.WARNING,
                    event.eventType(), event.eventType(), event.occurredAt(), Map.copyOf(metadata),
                    event.tenantId(), 1));
        } catch (IllegalArgumentException exception) {
            throw new PermanentEventFailureException("INVALID_GPS_EXCEPTION_NOTIFICATION_EVENT", exception.getMessage());
        }
    }

    private static String required(Object value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value.toString();
    }
}
