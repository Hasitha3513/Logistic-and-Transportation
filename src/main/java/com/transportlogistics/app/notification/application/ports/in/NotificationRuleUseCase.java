package com.transportlogistics.app.notification.application.ports.in;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.RecipientType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRuleUseCase {
    NotificationRule createRule(CreateRuleCommand command);
    NotificationRule updateRule(UUID id, UpdateRuleCommand command);
    NotificationRule enableRule(UUID id);
    NotificationRule disableRule(UUID id);
    void deleteRule(UUID id);
    Optional<NotificationRule> getRule(UUID id);
    List<NotificationRule> listRules();

    record CreateRuleCommand(
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {}

    record UpdateRuleCommand(
        String name,
        String description,
        String eventType,
        NotificationChannel channel,
        RecipientType recipientType,
        String recipientValue,
        boolean enabled,
        NotificationSeverity severityThreshold
    ) {}
}
