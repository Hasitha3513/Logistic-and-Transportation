package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.Notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    boolean existsByEventIdAndRuleIdAndRecipient(UUID eventId, UUID ruleId, String recipient);
    List<Notification> findByRecipientsOrderByCreatedAtDesc(Collection<String> recipients, int limit);
    long countUnreadByRecipients(Collection<String> recipients);
    int markAllAsReadForRecipients(Collection<String> recipients);
}
