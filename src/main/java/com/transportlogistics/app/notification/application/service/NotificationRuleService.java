package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class NotificationRuleService implements NotificationRuleUseCase {
    private final NotificationRuleRepository ruleRepository;

    public NotificationRuleService(NotificationRuleRepository ruleRepository) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository, "ruleRepository must not be null");
    }

    @Override
    public NotificationRule createRule(CreateRuleCommand command) {
        Objects.requireNonNull(command, "CreateRuleCommand must not be null");
        NotificationRule rule = NotificationRule.create(
            command.name(),
            command.description(),
            command.eventType(),
            command.channel(),
            command.recipientType(),
            command.recipientValue(),
            command.enabled(),
            command.severityThreshold()
        );
        return ruleRepository.save(rule);
    }

    @Override
    public NotificationRule updateRule(UUID id, UpdateRuleCommand command) {
        Objects.requireNonNull(id, "Rule ID must not be null");
        Objects.requireNonNull(command, "UpdateRuleCommand must not be null");
        NotificationRule existing = ruleRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Notification rule not found: " + id));

        NotificationRule updated = existing.update(
            command.name(),
            command.description(),
            command.eventType(),
            command.channel(),
            command.recipientType(),
            command.recipientValue(),
            command.enabled(),
            command.severityThreshold()
        );
        return ruleRepository.save(updated);
    }

    @Override
    public NotificationRule enableRule(UUID id) {
        Objects.requireNonNull(id, "Rule ID must not be null");
        NotificationRule existing = ruleRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Notification rule not found: " + id));
        return ruleRepository.save(existing.withEnabled(true));
    }

    @Override
    public NotificationRule disableRule(UUID id) {
        Objects.requireNonNull(id, "Rule ID must not be null");
        NotificationRule existing = ruleRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Notification rule not found: " + id));
        return ruleRepository.save(existing.withEnabled(false));
    }

    @Override
    public void deleteRule(UUID id) {
        Objects.requireNonNull(id, "Rule ID must not be null");
        if (!ruleRepository.existsById(id)) {
            throw new NoSuchElementException("Notification rule not found: " + id);
        }
        ruleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationRule> getRule(UUID id) {
        Objects.requireNonNull(id, "Rule ID must not be null");
        return ruleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRule> listRules() {
        return ruleRepository.findAll();
    }
}
