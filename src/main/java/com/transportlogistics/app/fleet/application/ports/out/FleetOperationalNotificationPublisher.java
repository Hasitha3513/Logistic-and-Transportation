package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.notification.OperationalNotificationEvent;

@FunctionalInterface
public interface FleetOperationalNotificationPublisher {
    void publish(OperationalNotificationEvent event);
}
