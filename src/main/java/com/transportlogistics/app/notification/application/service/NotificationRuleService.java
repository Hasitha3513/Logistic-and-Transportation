package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationTemplateRepository;
import com.transportlogistics.app.notification.domain.model.NotificationEventCatalogue;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.shared.domain.BusinessRuleException;
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
    private final NotificationTemplateRepository templateRepository;
    private final NotificationRecipientResolver recipientResolver;

    public NotificationRuleService(NotificationRuleRepository ruleRepository,
                                   NotificationTemplateRepository templateRepository,
                                   NotificationRecipientResolver recipientResolver) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository, "ruleRepository must not be null");
        this.templateRepository = Objects.requireNonNull(templateRepository, "templateRepository must not be null");
        this.recipientResolver = Objects.requireNonNull(recipientResolver, "recipientResolver must not be null");
    }

    @Override
    public NotificationRule createRule(CreateRuleCommand command) {
        Objects.requireNonNull(command, "CreateRuleCommand must not be null");
        String templateCode = validate(command.eventType(), command.channel(), command.templateCode(),
            command.recipientType(), command.recipientValue());
        NotificationRule rule = NotificationRule.create(
            command.name(),
            command.description(),
            command.eventType(),
            command.channel(),
            command.recipientType(),
            command.recipientValue(),
            templateCode,
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

        String templateCode = validate(command.eventType(), command.channel(), command.templateCode(),
            command.recipientType(), command.recipientValue());
        NotificationRule updated = existing.update(
            command.name(),
            command.description(),
            command.eventType(),
            command.channel(),
            command.recipientType(),
            command.recipientValue(),
            templateCode,
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
        validate(existing.eventType(), existing.channel(), existing.templateCode(),
            existing.recipientType(), existing.recipientValue());
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

    private String validate(String eventType,
                            com.transportlogistics.app.notification.domain.model.NotificationChannel channel,
                            String requestedTemplateCode,
                            com.transportlogistics.app.notification.domain.model.RecipientType recipientType,
                            String recipientValue) {
        var definition = NotificationEventCatalogue.find(eventType)
            .orElseThrow(() -> new BusinessRuleException("NOTIFICATION_EVENT_UNSUPPORTED",
                "Unsupported notification event: " + eventType));
        if (channel == null || !definition.supports(channel)) {
            throw new BusinessRuleException("NOTIFICATION_EVENT_UNSUPPORTED",
                "Notification event does not support channel " + channel);
        }
        String templateCode = requestedTemplateCode == null || requestedTemplateCode.isBlank()
            ? definition.templateCode()
            : requestedTemplateCode.trim().toUpperCase();
        if (!definition.templateCode().equals(templateCode)
            || templateRepository.findActiveCompatible(templateCode, definition.eventType(), channel).isEmpty()) {
            throw new BusinessRuleException("NOTIFICATION_TEMPLATE_INCOMPATIBLE",
                "No active compatible template for event and channel");
        }
        recipientResolver.validate(recipientType, channel, recipientValue);
        return templateCode;
    }
}
