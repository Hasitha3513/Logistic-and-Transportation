package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DeliveryCustomerNotificationEventBridge implements DurableEventHandler {
    private final OperationalNotificationPublisher notificationPublisher;

    public DeliveryCustomerNotificationEventBridge(OperationalNotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    public void onDeliveryEvent(DeliveryCustomerNotificationEvent event) {
        publish(event);
    }

    @Override
    public String consumerName() {
        return DeliveryCustomerNotificationEvent.DURABLE_CONSUMER;
    }

    @Override
    public void handle(DurableEventEnvelope envelope) {
        try {
            Map<String, String> payload = new LinkedHashMap<>();
            envelope.payload().forEach((key, value) -> {
                if (!(value instanceof String text)) {
                    throw new IllegalArgumentException("Delivery event payload values must be strings");
                }
                payload.put(key, text);
            });
            publish(new DeliveryCustomerNotificationEvent(envelope.eventId(), envelope.eventType(),
                envelope.tenantId(), envelope.occurredAt(), envelope.version(), envelope.aggregateType(),
                envelope.aggregateId(), payload));
        } catch (IllegalArgumentException exception) {
            throw new PermanentEventFailureException("INVALID_DELIVERY_EVENT", exception.getMessage());
        }
    }

    private void publish(DeliveryCustomerNotificationEvent event) {
        var severity = switch (event.eventType()) {
            case "DELIVERY_ETA_RISK_CHANGED", "DELIVERY_FAILED_ATTEMPT_RECORDED" ->
                OperationalNotificationEvent.Severity.WARNING;
            default -> OperationalNotificationEvent.Severity.INFO;
        };
        notificationPublisher.publish(new OperationalNotificationEvent(event.eventId(), event.eventType(),
            event.aggregateType(), event.aggregateId(), severity, event.eventType(), event.eventType(),
            event.occurredAt(), event.payload(), event.tenantId(), event.version()));
    }
}
