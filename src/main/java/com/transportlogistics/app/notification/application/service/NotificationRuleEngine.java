package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.OperationalNotificationEvent;
import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryPort;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRuleRepository;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
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
    private final Map<NotificationChannel, NotificationDeliveryPort> deliveryPorts;

    public NotificationRuleEngine(
        NotificationRuleRepository ruleRepository,
        NotificationRepository notificationRepository,
        Map<NotificationChannel, NotificationDeliveryPort> deliveryPorts
    ) {
        this.ruleRepository = Objects.requireNonNull(ruleRepository, "ruleRepository must not be null");
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
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
            if (!severity.meetsThreshold(rule.severityThreshold())) {
                log.debug("Rule '{}' skipped because event severity {} < threshold {}",
                    rule.name(), event.severity(), rule.severityThreshold());
                continue;
            }

            String recipient = resolveRecipient(rule);
            if (recipient == null || recipient.isBlank()) {
                log.warn("Could not resolve recipient for rule '{}', skipping.", rule.name());
                continue;
            }

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
                event.title(),
                event.message(),
                relatedRoute
            );

            Notification saved = notificationRepository.save(pending);
            dispatchNotification(saved, rule.channel());
        }
    }

    private NotificationSeverity toDomainSeverity(OperationalNotificationEvent.Severity severity) {
        return NotificationSeverity.valueOf(severity.name());
    }

    private String resolveRecipient(NotificationRule rule) {
        return switch (rule.recipientType()) {
            case USER -> rule.recipientValue();
            case ROLE -> {
                String r = rule.recipientValue().toUpperCase();
                yield r.startsWith("ROLE:") ? r : "ROLE:" + r;
            }
            case EMAIL_ADDRESS -> rule.recipientValue();
        };
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
