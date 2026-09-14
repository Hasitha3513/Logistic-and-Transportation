package com.transportlogistics.app.system.infrastructure.adapters.in.events;

import com.transportlogistics.app.notification.OperationalNotificationPublisher;
import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.DurableEventHandler;
import com.transportlogistics.app.shared.PermanentEventFailureException;
import com.transportlogistics.app.tracking.VehicleRouteDeviationDetectedV1;
import org.springframework.stereotype.Component;

@Component
public final class RouteDeviationDetectedNotificationBridge implements DurableEventHandler {
    private final OperationalNotificationPublisher notifications;
    public RouteDeviationDetectedNotificationBridge(OperationalNotificationPublisher notifications) {
        this.notifications = notifications;
    }
    @Override public String consumerName() { return VehicleRouteDeviationDetectedV1.CONSUMER; }
    @Override public void handle(DurableEventEnvelope envelope) {
        try {
            if (envelope == null || !VehicleRouteDeviationDetectedV1.EVENT_TYPE.equals(envelope.eventType())) {
                throw new IllegalArgumentException("Unsupported route-deviation detection event");
            }
            notifications.publish(RouteDeviationNotificationSupport.map(envelope, false));
        } catch (IllegalArgumentException exception) {
            throw new PermanentEventFailureException("INVALID_ROUTE_DEVIATION_DETECTED_EVENT", exception.getMessage());
        }
    }
}
