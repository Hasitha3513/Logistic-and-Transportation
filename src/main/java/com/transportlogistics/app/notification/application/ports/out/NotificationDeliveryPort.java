package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.Notification;

public interface NotificationDeliveryPort {
    void deliver(Notification notification);
}
