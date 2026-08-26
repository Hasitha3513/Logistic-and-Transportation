package com.transportlogistics.app.notification.application.ports.in;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationEscalationUseCase {
    void escalateIfDue(UUID notificationId, OffsetDateTime now);
}
