package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationConfigurationUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationTemplateRepository;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationEventCatalogue;
import com.transportlogistics.app.notification.domain.model.NotificationEventDefinition;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationConfigurationService implements NotificationConfigurationUseCase {
    private final NotificationTemplateRepository templates;

    public NotificationConfigurationService(NotificationTemplateRepository templates) {
        this.templates = Objects.requireNonNull(templates, "templates must not be null");
    }

    @Override
    public List<NotificationEventDefinition> listEventCatalogue() {
        return NotificationEventCatalogue.all();
    }

    @Override
    public List<NotificationTemplate> listActiveTemplates(String eventType, NotificationChannel channel) {
        if (eventType != null && NotificationEventCatalogue.find(eventType).isEmpty()) {
            throw new BusinessRuleException("NOTIFICATION_EVENT_UNSUPPORTED", "Unsupported notification event: " + eventType);
        }
        return templates.findActive(eventType, channel);
    }

    @Override
    public Optional<NotificationTemplate> getActiveTemplate(UUID id) {
        return templates.findById(id).filter(NotificationTemplate::active);
    }
}
