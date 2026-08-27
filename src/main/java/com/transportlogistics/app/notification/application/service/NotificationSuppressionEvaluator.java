package com.transportlogistics.app.notification.application.service;

import com.transportlogistics.app.notification.application.ports.out.NotificationRuleExecutionRepository;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;
import com.transportlogistics.app.notification.domain.model.NotificationRulePolicy;
import com.transportlogistics.app.notification.domain.model.NotificationSeverity;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
public class NotificationSuppressionEvaluator {
    private final NotificationRuleExecutionRepository repository;

    public NotificationSuppressionEvaluator(NotificationRuleExecutionRepository repository) {
        this.repository = repository;
    }

    public SuppressionDecision evaluate(NotificationRulePolicy policy, NotificationSeverity severity,
                                        String suppressionKey, OffsetDateTime now) {
        if (severity == NotificationSeverity.CRITICAL || policy.suppressionWindowMinutes() == 0) {
            return SuppressionDecision.accepted();
        }
        Optional<NotificationRuleExecution> controlling = repository.findLatestAccepted(suppressionKey,
            now.minusMinutes(policy.suppressionWindowMinutes()));
        return controlling.map(SuppressionDecision::suppressed).orElseGet(SuppressionDecision::accepted);
    }

    public record SuppressionDecision(boolean suppressed, NotificationRuleExecution controllingExecution) {
        static SuppressionDecision accepted() { return new SuppressionDecision(false, null); }
        static SuppressionDecision suppressed(NotificationRuleExecution execution) {
            return new SuppressionDecision(true, execution);
        }
    }
}
