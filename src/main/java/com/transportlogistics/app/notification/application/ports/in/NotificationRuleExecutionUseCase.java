package com.transportlogistics.app.notification.application.ports.in;

import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;

import java.util.List;
import java.util.UUID;

public interface NotificationRuleExecutionUseCase {
    List<NotificationRuleExecution> listExecutions(UUID ruleId, UUID eventId, int limit);
}
