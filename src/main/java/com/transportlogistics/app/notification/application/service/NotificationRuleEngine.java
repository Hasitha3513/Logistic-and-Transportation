package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationTemplateRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import com.transportlogistics.app.notification.domain.model.NotificationTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationRuleEngine {
    private static final Logger log = LoggerFactory.getLogger(NotificationRuleEngine.class);

    private final NotificationRuleRepository ruleRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateRenderer templateRenderer;
    private final Map<NotificationChannel, NotificationDeliveryPort> deliveryPorts;

    public NotificationRuleEngine(
        NotificationRuleRepository ruleRepository,
        NotificationRepository notificationRepository,
        NotificationTemplateRepository templateRepository,
        NotificationRecipientResolver recipientResolver,
        NotificationTemplateRenderer templateRenderer,
        Map<NotificationChannel, NotificationDeliveryPort> deliveryPorts
    ) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository, "ruleRepository must not be null");
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
        this.templateRepository = Objects.requireNonNull(templateRepository, "templateRepository must not be null");
        this.recipientResolver = Objects.requireNonNull(recipientResolver, "recipientResolver must not be null");
        this.templateRenderer = Objects.requireNonNull(templateRenderer, "templateRenderer must not be null");
        this.deliveryPorts = Objects.requireNonNull(deliveryPorts, "deliveryPorts must not be null");
    }

    @Transactional
    public void processEvent(OperationalNotificationEvent event) {
        if (event == null) {
            log.warn("Received null OperationalNotificationEvent, skipping processing.");
            return;
        }

        log.info("Evaluating notification rules for event: {} (severity: {}, aggregate: {}/{})",
            event.eventType(), event.severity(), event.aggregateType(), event.aggregateId());

        List<NotificationRule> matchingRules = ruleRepository.findByEventTypeAndEnabledTrue(event.eventType());
        if (matchingRules.isEmpty()) {
            log.debug("No enabled notification rules matching event type '{}'", event.eventType());
            return;
        }

        NotificationSeverity severity = toDomainSeverity(event.severity());
        for (NotificationRule rule : matchingRules) {
            try {
                processRule(event, severity, rule);
            } catch (RuntimeException exception) {
                log.error("Notification rule {} failed for event {} without affecting the source operation: {}",
                    rule.id(), event.eventId(), exception.getMessage(), exception);
            }
        }
    }

    private void processRule(OperationalNotificationEvent event, NotificationSeverity severity, NotificationRule rule) {
        if (!severity.meetsThreshold(rule.severityThreshold())) {
            log.debug("Rule '{}' skipped because event severity {} < threshold {}",
                rule.name(), event.severity(), rule.severityThreshold());
            return;
        }

        if (rule.templateCode() == null) {
            log.warn("Legacy notification rule '{}' has no template and cannot execute", rule.name());
            return;
        }

        var template = templateRepository.findActiveCompatible(rule.templateCode(), rule.eventType(), rule.channel())
            .orElseThrow(() -> new IllegalStateException("No active compatible template for rule " + rule.id()));
        Map<String, String> variables = new java.util.HashMap<>(event.metadata());
        variables.put("eventTime", event.occurredAt().toString());
        variables.put("severity", event.severity().name());
        var rendered = templateRenderer.render(template, variables);

        List<String> recipients = recipientResolver.resolve(rule.recipientType(), rule.channel(), rule.recipientValue());
        if (recipients.isEmpty()) {
            log.warn("Rule '{}' resolved no active recipients; NO_RECIPIENT", rule.name());
            return;
        }

        for (String recipient : recipients) {

            // Enforce idempotency: eventId + ruleId + recipient
            if (notificationRepository.existsByEventIdAndRuleIdAndRecipient(event.eventId(), rule.id(), recipient)) {
                log.debug("Duplicate notification prevented for eventId: {}, ruleId: {}, recipient: {}",
                    event.eventId(), rule.id(), recipient);
                continue;
            }

            String relatedRoute = event.metadata() != null ? event.metadata().get("relatedRoute") : null;
            Notification pending = Notification.createPending(
                rule.id(),
                event.eventId(),
                event.eventType(),
                rule.channel(),
                recipient,
                severity,
                rendered.subject(),
                rendered.body(),
                template.id(),
                template.version(),
                relatedRoute
            );

            Notification saved = notificationRepository.save(pending);
            dispatchNotification(saved, rule.channel());
        }
    }

    private NotificationSeverity toDomainSeverity(OperationalNotificationEvent.Severity severity) {
        return NotificationSeverity.valueOf(severity.name());
    }

    private void dispatchNotification(Notification notification, NotificationChannel channel) {
        NotificationDeliveryPort port = deliveryPorts.get(channel);
        if (port == null) {
            log.error("No delivery adapter registered for channel: {}", channel);
            notificationRepository.save(notification.markFailed("No delivery adapter registered for channel " + channel));
            return;
        }

        try {
            port.deliver(notification);
            notificationRepository.save(notification.markSent());
            log.info("Dispatched {} notification {} to {}", channel, notification.id(), notification.recipient());
        } catch (Exception ex) {
            log.error("Failed to deliver {} notification {}: {}", channel, notification.id(), ex.getMessage(), ex);
            notificationRepository.save(notification.markFailed(ex.getMessage()));
        }
    }
}
