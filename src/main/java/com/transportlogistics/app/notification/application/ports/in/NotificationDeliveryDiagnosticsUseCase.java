package com.transportlogistics.app.notification.application.ports.in;

import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryDiagnosticsUseCase {
    List<DeliveryDiagnostic> find(NotificationStatus status, String eventType, OffsetDateTime from,
                                  OffsetDateTime to, int limit);
    List<NotificationDeliveryAttempt> attempts(UUID notificationId);

    record DeliveryDiagnostic(Notification notification, long attemptCount) {}
}
