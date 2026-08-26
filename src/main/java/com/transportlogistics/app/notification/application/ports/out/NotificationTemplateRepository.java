package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository {
    Optional<NotificationTemplate> findById(UUID id);

    Optional<NotificationTemplate> findActiveCompatible(String code, String eventType, NotificationChannel channel);

    List<NotificationTemplate> findActive(String eventType, NotificationChannel channel);
}
