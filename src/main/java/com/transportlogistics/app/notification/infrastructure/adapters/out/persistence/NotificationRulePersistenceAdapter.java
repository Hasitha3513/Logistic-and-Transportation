package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationRulePersistenceAdapter implements NotificationRuleRepository {
    private final NotificationRuleJpaRepository jpaRepository;

    public NotificationRulePersistenceAdapter(NotificationRuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NotificationRule save(NotificationRule rule) {
        NotificationRuleEntity entity = NotificationRuleEntity.fromDomain(rule);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<NotificationRule> findById(UUID id) {
        return jpaRepository.findById(id).map(NotificationRuleEntity::toDomain);
    }

    @Override
    public List<NotificationRule> findAll() {
        return jpaRepository.findAll().stream().map(NotificationRuleEntity::toDomain).toList();
    }

    @Override
    public List<NotificationRule> findByEventTypeAndEnabledTrue(String eventType) {
        return jpaRepository.findByEventTypeIgnoreCaseAndEnabledTrue(eventType).stream()
            .map(NotificationRuleEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
