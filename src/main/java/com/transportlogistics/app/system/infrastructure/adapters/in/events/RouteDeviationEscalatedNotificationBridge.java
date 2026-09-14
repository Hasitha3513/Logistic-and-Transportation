package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.tracking.VehicleRouteDeviationEscalatedV1;
import org.springframework.stereotype.Component;

@Component
public final class RouteDeviationEscalatedNotificationBridge implements DurableEventHandler {
    private final OperationalNotificationPublisher notifications;
    public RouteDeviationEscalatedNotificationBridge(OperationalNotificationPublisher notifications) {
        this.notifications = notifications;
    }
    @Override public String consumerName() { return VehicleRouteDeviationEscalatedV1.CONSUMER; }
    @Override public void handle(DurableEventEnvelope envelope) {
        try {
            if (envelope == null || !VehicleRouteDeviationEscalatedV1.EVENT_TYPE.equals(envelope.eventType())) {
                throw new IllegalArgumentException("Unsupported route-deviation escalation event");
            }
            notifications.publish(RouteDeviationNotificationSupport.map(envelope, true));
        } catch (IllegalArgumentException exception) {
            throw new PermanentEventFailureException("INVALID_ROUTE_DEVIATION_ESCALATED_EVENT", exception.getMessage());
        }
    }
}
