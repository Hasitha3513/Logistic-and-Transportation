package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationDeliveryAttemptRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRepository;
import com.transportlogistics.app.notification.application.ports.out.NotificationRulePolicyRepository;
import com.transportlogistics.app.notification.application.ports.in.NotificationEscalationUseCase;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationEscalationService implements NotificationEscalationUseCase {
    private static final Logger log = LoggerFactory.getLogger(NotificationEscalationService.class);
    private static final String COMPLETED_MARKER = "ESCALATION_NO_RECIPIENT";
    private final NotificationRepository notifications;
    private final NotificationDeliveryAttemptRepository attempts;
    private final NotificationRulePolicyRepository policies;
    private final NotificationRecipientResolver recipients;

    public NotificationEscalationService(NotificationRepository notifications,
                                         NotificationDeliveryAttemptRepository attempts,
                                         NotificationRulePolicyRepository policies,
                                         NotificationRecipientResolver recipients) {
        this.notifications = notifications; this.attempts = attempts; this.policies = policies; this.recipients = recipients;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void escalateIfDue(UUID notificationId, OffsetDateTime now) {
        Notification failed = notifications.findByIdForUpdate(notificationId).orElse(null);
        if (failed == null || failed.status() != com.transportlogistics.app.notification.domain.model.NotificationStatus.FAILED
            || failed.channel() != NotificationChannel.EMAIL || failed.escalationLevel() != 0
            || failed.failureReason() != null && failed.failureReason().contains(COMPLETED_MARKER)
            || failed.ruleId() == null) return;
        var policy = policies.findByRuleId(failed.ruleId()).orElse(null);
        if (policy == null || !policy.escalationEnabled()) return;
        var latest = attempts.findLatest(failed.id()).orElse(null);
        if (latest == null || latest.completedAt() == null
            || latest.completedAt().plusMinutes(policy.escalationDelayMinutes()).isAfter(now)) return;

        List<String> resolved;
        try {
            resolved = recipients.resolve(policy.escalationRecipientType(), NotificationChannel.IN_APP,
                policy.escalationRecipientValue());
        } catch (RuntimeException exception) {
            resolved = List.of();
            log.warn("Escalation recipient could not be resolved for notification {}", failed.id());
        }
        if (resolved.isEmpty()) {
            notifications.save(failed.recordEscalationDiagnostic(COMPLETED_MARKER));
            return;
        }
        for (String recipient : resolved.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
            if (!notifications.existsByParentNotificationIdAndRecipient(failed.id(), recipient)) {
                notifications.save(Notification.createEscalation(failed, recipient, failed.failureReason(), now));
            }
        }
    }
}
