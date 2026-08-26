package com.transportlogistics.app.notification.application.ports.in;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationEventDefinition;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationConfigurationUseCase {
    List<NotificationEventDefinition> listEventCatalogue();

    List<NotificationTemplate> listActiveTemplates(String eventType, NotificationChannel channel);

    Optional<NotificationTemplate> getActiveTemplate(UUID id);
}
