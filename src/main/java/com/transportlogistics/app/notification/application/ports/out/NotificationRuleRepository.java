package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.NotificationRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRuleRepository {
    NotificationRule save(NotificationRule rule);
    Optional<NotificationRule> findById(UUID id);
    List<NotificationRule> findAll();
    List<NotificationRule> findByEventTypeAndEnabledTrue(String eventType);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
