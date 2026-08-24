package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleExecutionUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleExecutionRepository;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationRuleExecutionService implements NotificationRuleExecutionUseCase {
    private final NotificationRuleExecutionRepository repository;

    public NotificationRuleExecutionService(NotificationRuleExecutionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRuleExecution> listExecutions(UUID ruleId, UUID eventId, int limit) {
        return repository.findRecent(ruleId, eventId, limit);
    }
}
