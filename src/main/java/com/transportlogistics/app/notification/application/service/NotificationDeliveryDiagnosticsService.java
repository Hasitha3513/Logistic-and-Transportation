package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationDeliveryDiagnosticsUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryAttemptRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import com.transportlogistics.app.shared.domain.NotFoundException;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationDeliveryDiagnosticsService implements NotificationDeliveryDiagnosticsUseCase {
    private final NotificationRepository notifications;
    private final NotificationDeliveryAttemptRepository attempts;

    public NotificationDeliveryDiagnosticsService(NotificationRepository notifications,
                                                  NotificationDeliveryAttemptRepository attempts) {
        this.notifications = notifications; this.attempts = attempts;
    }

    public List<DeliveryDiagnostic> find(NotificationStatus status, String eventType, OffsetDateTime from,
                                         OffsetDateTime to, String aggregateType, UUID aggregateId, int limit) {
        int bounded = Math.max(1, Math.min(limit, 200));
        return notifications.findDeliveries(status, eventType, from, to, aggregateType, aggregateId, bounded).stream()
            .map(notification -> new DeliveryDiagnostic(notification, attempts.countByNotificationId(notification.id())))
            .toList();
    }

    public List<DeliveryDiagnostic> find(NotificationStatus status, String eventType, OffsetDateTime from,
                                         OffsetDateTime to, int limit) {
        int bounded = Math.max(1, Math.min(limit, 200));
        return notifications.findDeliveries(status, eventType, from, to, bounded).stream()
            .map(notification -> new DeliveryDiagnostic(notification, attempts.countByNotificationId(notification.id())))
            .toList();
    }

    public List<NotificationDeliveryAttempt> attempts(UUID notificationId) {
        if (notifications.findById(notificationId).isEmpty()) {
            throw new NotFoundException("NOTIFICATION_DELIVERY_NOT_FOUND",
                "Notification delivery not found: " + notificationId);
        }
        return attempts.findByNotificationId(notificationId);
    }
}
