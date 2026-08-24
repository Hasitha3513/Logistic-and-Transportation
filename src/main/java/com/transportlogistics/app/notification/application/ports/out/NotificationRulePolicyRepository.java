package com.transportlogistics.app.notification.application.ports.out;

import com.transportlogistics.app.notification.domain.model.NotificationRulePolicy;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRulePolicyRepository {
    NotificationRulePolicy save(UUID ruleId, NotificationRulePolicy policy);
    Optional<NotificationRulePolicy> findByRuleId(UUID ruleId);
    Optional<NotificationRulePolicy> findByRuleIdForUpdate(UUID ruleId);
}
