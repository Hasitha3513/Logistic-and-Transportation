package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRuleJpaRepository extends JpaRepository<NotificationRuleEntity, UUID> {
    List<NotificationRuleEntity> findByEventTypeIgnoreCaseAndEnabledTrue(String eventType);
}
