package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.ports.out.*;
import com.transportlogistics.app.notification.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationRuleEngine {
    private static final Logger log = LoggerFactory.getLogger(NotificationRuleEngine.class);
    private final NotificationRuleRepository ruleRepository;
    private final NotificationRulePolicyRepository policyRepository;
    private final NotificationRuleExecutionRepository executionRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationRecipientResolver recipientResolver;
    private final EventCustomerRecipientResolver eventCustomerRecipientResolver;
    private final NotificationTemplateRenderer templateRenderer;
    private final NotificationQuietHoursEvaluator quietHoursEvaluator;
    private final NotificationSuppressionEvaluator suppressionEvaluator;
    private final Map<NotificationChannel, NotificationDeliveryPort> deliveryPorts;
    private final Clock clock;

    @Autowired
    public NotificationRuleEngine(NotificationRuleRepository ruleRepository,
                                  NotificationRulePolicyRepository policyRepository,
                                  NotificationRuleExecutionRepository executionRepository,
                                  NotificationRepository notificationRepository,
                                  NotificationTemplateRepository templateRepository,
                                  NotificationRecipientResolver recipientResolver,
                                  EventCustomerRecipientResolver eventCustomerRecipientResolver,
                                  NotificationTemplateRenderer templateRenderer,
                                  NotificationQuietHoursEvaluator quietHoursEvaluator,
                                  NotificationSuppressionEvaluator suppressionEvaluator,
                                  Map<NotificationChannel, NotificationDeliveryPort> deliveryPorts,
                                  Clock clock) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.executionRepository = Objects.requireNonNull(executionRepository);
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
        this.templateRepository = Objects.requireNonNull(templateRepository);
        this.recipientResolver = Objects.requireNonNull(recipientResolver);
        this.eventCustomerRecipientResolver = Objects.requireNonNull(eventCustomerRecipientResolver);
        this.templateRenderer = Objects.requireNonNull(templateRenderer);
        this.quietHoursEvaluator = Objects.requireNonNull(quietHoursEvaluator);
        this.suppressionEvaluator = Objects.requireNonNull(suppressionEvaluator);
        this.deliveryPorts = Objects.requireNonNull(deliveryPorts);
        this.clock = Objects.requireNonNull(clock);
    }

    NotificationRuleEngine(NotificationRuleRepository ruleRepository,
                           NotificationRulePolicyRepository policyRepository,
                           NotificationRuleExecutionRepository executionRepository,
                           NotificationRepository notificationRepository,
                           NotificationTemplateRepository templateRepository,
                           NotificationRecipientResolver recipientResolver,
                           NotificationTemplateRenderer templateRenderer,
                           NotificationQuietHoursEvaluator quietHoursEvaluator,
                           NotificationSuppressionEvaluator suppressionEvaluator,
                           Map<NotificationChannel, NotificationDeliveryPort> deliveryPorts,
                           Clock clock) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.executionRepository = Objects.requireNonNull(executionRepository);
        this.notificationRepository = Objects.requireNonNull(notificationRepository);
        this.templateRepository = Objects.requireNonNull(templateRepository);
        this.recipientResolver = Objects.requireNonNull(recipientResolver);
        this.eventCustomerRecipientResolver = null;
        this.templateRenderer = Objects.requireNonNull(templateRenderer);
        this.quietHoursEvaluator = Objects.requireNonNull(quietHoursEvaluator);
        this.suppressionEvaluator = Objects.requireNonNull(suppressionEvaluator);
        this.deliveryPorts = Objects.requireNonNull(deliveryPorts);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(OperationalNotificationEvent event) {
        if (event == null) {
            log.warn("Received null OperationalNotificationEvent, skipping processing");
            return;
        }
        NotificationSeverity severity = NotificationSeverity.valueOf(event.severity().name());
        for (NotificationRule rule : ruleRepository.findByEventTypeAndEnabledTrue(event.eventType())) {
            if (!severity.meetsThreshold(rule.severityThreshold())) continue;
            try {
                processRule(event, severity, rule);
            } catch (RuntimeException exception) {
                log.error("Notification rule {} failed for event {} without affecting the source operation: {}",
                    rule.id(), event.eventId(), exception.getMessage(), exception);
                record(event, rule, null, NotificationRuleExecutionOutcome.FAILED, null, null,
                    "RULE_EVALUATION_FAILED", exception.getMessage(), now());
            }
        }
    }

    private void processRule(OperationalNotificationEvent event, NotificationSeverity severity, NotificationRule rule) {
        var policy = policyRepository.findByRuleIdForUpdate(rule.id()).orElse(rule.policy());
        OffsetDateTime now = now();
        var template = templateRepository.findActiveCompatible(rule.templateCode(), rule.eventType(), rule.channel()).orElse(null);
        if (template == null) {
            record(event, rule, null, NotificationRuleExecutionOutcome.TEMPLATE_DATA_MISSING, null, null,
                "TEMPLATE_DATA_MISSING", "No active compatible template", now);
            return;
        }
        Map<String, String> variables = new HashMap<>(event.metadata());
        String eventCustomerRecipient = null;
        if (rule.recipientType() == RecipientType.EVENT_CUSTOMER) {
            if (eventCustomerRecipientResolver == null) {
                throw new IllegalStateException("EVENT_CUSTOMER recipient resolver is unavailable");
            }
            var resolution = eventCustomerRecipientResolver.resolve(event, rule.channel());
            if (resolution.state() == EventCustomerRecipientResolver.State.SUPPRESSED) {
                record(event, rule, null, NotificationRuleExecutionOutcome.SUPPRESSED, null, null,
                    "CUSTOMER_CHANNEL_DISABLED", "Customer disabled this notification channel", now);
                return;
            }
            if (resolution.state() == EventCustomerRecipientResolver.State.NO_RECIPIENT) {
                record(event, rule, null, NotificationRuleExecutionOutcome.NO_RECIPIENT, null, null,
                    "NO_RECIPIENT", "No active customer destination resolved", now);
                return;
            }
            variables.putAll(resolution.variables());
            eventCustomerRecipient = resolution.recipient();
        }
        List<String> recipients = rule.recipientType() == RecipientType.EVENT_CUSTOMER
            ? List.of(eventCustomerRecipient)
            : recipientResolver.resolve(rule.recipientType(), rule.channel(), rule.recipientValue());
        if (recipients.isEmpty()) {
            record(event, rule, null, NotificationRuleExecutionOutcome.NO_RECIPIENT, null, null,
                "NO_RECIPIENT", "No active recipient resolved", now);
            return;
        }

        NotificationTemplateRenderer.RenderedNotification rendered;
        try {
            variables.put("eventTime", event.occurredAt().toString());
            variables.put("severity", event.severity().name());
            rendered = templateRenderer.render(template, variables);
        } catch (IllegalArgumentException exception) {
            for (String recipient : recipients) record(event, rule, recipient,
                NotificationRuleExecutionOutcome.TEMPLATE_DATA_MISSING, null, null,
                "TEMPLATE_DATA_MISSING", exception.getMessage(), now);
            return;
        }

        String milestone = NotificationEventCatalogue.require(event.eventType()).milestone(event.metadata());
        for (String recipient : recipients) {
            String executionKey = NotificationRuleExecution.executionKey(event.eventId(), rule.id(), rule.channel(), recipient);
            if (executionRepository.existsByExecutionKey(executionKey)) continue;
            String suppressionKey = NotificationSuppressionKey.of(rule.id(), event.eventType(), event.aggregateType(),
                event.aggregateId(), recipient, rule.channel(), milestone).value();
            var suppression = suppressionEvaluator.evaluate(policy, severity, suppressionKey, now);
            if (suppression.suppressed()) {
                record(event, rule, recipient, NotificationRuleExecutionOutcome.SUPPRESSED, suppressionKey,
                    suppression.controllingExecution().controllingNotificationId(), null, null, now);
                continue;
            }

            var quietDecision = quietHoursEvaluator.evaluate(policy, rule.channel(), severity);
            Notification pending = Notification.createPending(rule.id(), event.eventId(), event.eventType(), rule.channel(),
                recipient, severity, rendered.subject(), rendered.body(), template.id(), template.version(),
                event.metadata().get("relatedRoute"), now, quietDecision.nextDeliveryAt());
            Notification saved = notificationRepository.save(pending);
            if (rule.channel() == NotificationChannel.IN_APP) dispatchNotification(saved, rule.channel());
            record(event, rule, recipient, NotificationRuleExecutionOutcome.ACCEPTED, suppressionKey,
                saved.id(), null, null, now);
        }
    }

    private void record(OperationalNotificationEvent event, NotificationRule rule, String recipient,
                        NotificationRuleExecutionOutcome outcome, String suppressionKey,
                        UUID controllingNotificationId, String failureCode, String failureMessage,
                        OffsetDateTime timestamp) {
        String key = NotificationRuleExecution.executionKey(event.eventId(), rule.id(), rule.channel(), recipient);
        if (executionRepository.existsByExecutionKey(key)) return;
        executionRepository.save(NotificationRuleExecution.completed(event.eventId(), event.eventType(),
            event.aggregateType(), event.aggregateId(), rule.id(), recipient, rule.channel(), outcome,
            suppressionKey, controllingNotificationId, failureCode, failureMessage, timestamp));
    }

    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }

    private void dispatchNotification(Notification notification, NotificationChannel channel) {
        NotificationDeliveryPort port = deliveryPorts.get(channel);
        if (port == null) {
            notificationRepository.save(notification.markFailed("No delivery adapter registered for channel " + channel));
            return;
        }
        try {
            port.deliver(notification);
            notificationRepository.save(notification.markSent());
        } catch (Exception exception) {
            notificationRepository.save(notification.markFailed(exception.getMessage()));
        }
    }
}
