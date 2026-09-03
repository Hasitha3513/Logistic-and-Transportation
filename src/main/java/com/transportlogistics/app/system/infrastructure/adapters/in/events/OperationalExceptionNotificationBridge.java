package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.operations.OperationalExceptionEscalatedV1;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OperationalExceptionNotificationBridge implements DurableEventHandler {
    private final OperationalNotificationPublisher notifications;
    public OperationalExceptionNotificationBridge(OperationalNotificationPublisher notifications) {
        this.notifications = notifications;
    }
    @Override public String consumerName() { return OperationalExceptionEscalatedV1.CONSUMER; }

    @Override
    public void handle(DurableEventEnvelope event) {
        try {
            if (!OperationalExceptionEscalatedV1.EVENT_TYPE.equals(event.eventType()) || event.version() != 1) {
                throw new IllegalArgumentException("Unsupported operational escalation event");
            }
            Map<String, String> metadata = new LinkedHashMap<>();
            event.payload().forEach((key, value) -> metadata.put(key, String.valueOf(value)));
            String severity = metadata.get("severity");
            var notificationSeverity = "CRITICAL".equals(severity)
                ? OperationalNotificationEvent.Severity.CRITICAL : OperationalNotificationEvent.Severity.WARNING;
            String reference = metadata.get("caseReference");
            notifications.publish(new OperationalNotificationEvent(event.eventId(), event.eventType(),
                event.aggregateType(), event.aggregateId(), notificationSeverity,
                "Operational exception escalated", "Operational exception " + reference + " requires attention",
                event.occurredAt(), Map.copyOf(metadata), event.tenantId(), event.version()));
        } catch (IllegalArgumentException exception) {
            throw new PermanentEventFailureException("INVALID_OPERATIONAL_ESCALATION_EVENT", exception.getMessage());
        }
    }
}
