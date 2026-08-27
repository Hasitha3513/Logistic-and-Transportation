package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.NotificationTemplateRepository;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationTemplatePersistenceAdapter implements NotificationTemplateRepository {
    private final NotificationTemplateJpaRepository repository;

    public NotificationTemplatePersistenceAdapter(NotificationTemplateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<NotificationTemplate> findById(UUID id) {
        return repository.findById(id).map(NotificationTemplateEntity::toDomain);
    }

    @Override
    public Optional<NotificationTemplate> findActiveCompatible(String code, String eventType, NotificationChannel channel) {
        List<NotificationTemplateEntity> matches = repository
            .findByCodeIgnoreCaseAndEventTypeIgnoreCaseAndChannelAndActiveTrue(code, eventType, channel);
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple active notification template versions for " + code + "/" + channel);
        }
        return matches.stream().findFirst().map(NotificationTemplateEntity::toDomain);
    }

    @Override
    public List<NotificationTemplate> findActive(String eventType, NotificationChannel channel) {
        List<NotificationTemplateEntity> entities;
        if (eventType != null && channel != null) {
            entities = repository.findByEventTypeIgnoreCaseAndChannelAndActiveTrueOrderByVersionDesc(eventType, channel);
        } else if (eventType != null) {
            entities = repository.findByEventTypeIgnoreCaseAndActiveTrueOrderByChannelAscVersionDesc(eventType);
        } else if (channel != null) {
            entities = repository.findByChannelAndActiveTrueOrderByEventTypeAscVersionDesc(channel);
        } else {
            entities = repository.findByActiveTrueOrderByEventTypeAscChannelAscVersionDesc();
        }
        return entities.stream().map(NotificationTemplateEntity::toDomain).toList();
    }
}
