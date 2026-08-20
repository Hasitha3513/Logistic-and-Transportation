package com.transportlogistics.app.notification.infrastructure.adapters.out.inapp;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.domain.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("inAppNotificationDeliveryAdapter")
public class InAppNotificationDeliveryAdapter implements NotificationDeliveryPort {
    private static final Logger log = LoggerFactory.getLogger(InAppNotificationDeliveryAdapter.class);

    @Override
    public void deliver(Notification notification) {
        log.info("In-App notification prepared for recipient '{}': [{}] {}",
            notification.recipient(), notification.severity(), notification.title());
    }
}
