package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.delivery.DeliveryCustomerNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryCustomerNotificationEventBridge {
    private static final Logger log = LoggerFactory.getLogger(DeliveryCustomerNotificationEventBridge.class);
    private final OperationalNotificationPublisher notificationPublisher;

    public DeliveryCustomerNotificationEventBridge(OperationalNotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @EventListener
    public void onDeliveryEvent(DeliveryCustomerNotificationEvent event) {
        try {
            var severity = switch (event.eventType()) {
                case "DELIVERY_ETA_RISK_CHANGED", "DELIVERY_FAILED_ATTEMPT_RECORDED" ->
                    OperationalNotificationEvent.Severity.WARNING;
                default -> OperationalNotificationEvent.Severity.INFO;
            };
            notificationPublisher.publish(new OperationalNotificationEvent(event.eventId(), event.eventType(),
                event.aggregateType(), event.aggregateId(), severity, event.eventType(), event.eventType(),
                event.occurredAt(), event.payload(), event.tenantId(), event.version()));
        } catch (RuntimeException exception) {
            log.error("Delivery notification event {} failed without affecting Delivery", event.eventId(), exception);
        }
    }
}
