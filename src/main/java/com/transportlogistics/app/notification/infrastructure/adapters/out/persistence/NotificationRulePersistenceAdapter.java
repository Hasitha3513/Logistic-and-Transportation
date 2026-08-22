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
    private final NotificationRulePolicyJpaRepository policyRepository;

    public NotificationRulePersistenceAdapter(NotificationRuleJpaRepository jpaRepository,
                                              NotificationRulePolicyJpaRepository policyRepository) {
        this.jpaRepository = jpaRepository;
        this.policyRepository = policyRepository;
    }

    @Override
    public NotificationRule save(NotificationRule rule) {
        NotificationRuleEntity entity = NotificationRuleEntity.fromDomain(rule);
        NotificationRuleEntity saved = jpaRepository.save(entity);
        NotificationRulePolicyEntity policy = policyRepository.findById(rule.id())
            .orElseGet(() -> NotificationRulePolicyEntity.fromDomain(rule.id(), rule.policy()));
        policy.apply(rule.policy());
        policyRepository.save(policy);
        return saved.toDomain(policy.toDomain());
    }

    @Override
    public Optional<NotificationRule> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<NotificationRule> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<NotificationRule> findByEventTypeAndEnabledTrue(String eventType) {
        return jpaRepository.findByEventTypeIgnoreCaseAndEnabledTrue(eventType).stream()
            .map(this::toDomain)
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

    private NotificationRule toDomain(NotificationRuleEntity entity) {
        var policy = policyRepository.findById(entity.getId())
            .map(NotificationRulePolicyEntity::toDomain)
            .orElseGet(() -> com.transportlogistics.app.notification.domain.model.NotificationRulePolicy.defaults(entity.getEventType()));
        return entity.toDomain(policy);
    }
}
