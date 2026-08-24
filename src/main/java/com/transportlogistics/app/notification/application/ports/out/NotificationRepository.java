package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.Notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    boolean existsByEventIdAndRuleIdAndRecipient(UUID eventId, UUID ruleId, String recipient);
    List<Notification> findByRecipientsOrderByCreatedAtDesc(Collection<String> recipients, int limit);
    long countUnreadByRecipients(Collection<String> recipients);
    int markAllAsReadForRecipients(Collection<String> recipients);
    Optional<Notification> findByIdForUpdate(UUID id);
    List<Notification> findDuePendingEmails(OffsetDateTime now, int limit);
    List<Notification> findFailedEmails(int limit);
    List<Notification> findDeliveries(NotificationStatus status, String eventType,
                                      OffsetDateTime from, OffsetDateTime to, int limit);
    boolean existsByParentNotificationIdAndRecipient(UUID parentNotificationId, String recipient);
}
