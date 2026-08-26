package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.application.ports.out.NotificationRulePolicyRepository;
import com.transportlogistics.app.notification.domain.model.NotificationRulePolicy;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationRulePolicyPersistenceAdapter implements NotificationRulePolicyRepository {
    private final NotificationRulePolicyJpaRepository repository;

    public NotificationRulePolicyPersistenceAdapter(NotificationRulePolicyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationRulePolicy save(UUID ruleId, NotificationRulePolicy policy) {
        NotificationRulePolicyEntity entity = repository.findById(ruleId)
            .orElseGet(() -> NotificationRulePolicyEntity.fromDomain(ruleId, policy));
        entity.apply(policy);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<NotificationRulePolicy> findByRuleId(UUID ruleId) {
        return repository.findById(ruleId).map(NotificationRulePolicyEntity::toDomain);
    }

    @Override
    public Optional<NotificationRulePolicy> findByRuleIdForUpdate(UUID ruleId) {
        return repository.findByRuleIdForUpdate(ruleId).map(NotificationRulePolicyEntity::toDomain);
    }
}
