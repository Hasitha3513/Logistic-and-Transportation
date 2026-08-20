package com.transportlogistics.app.notification.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.CreateNotificationRuleRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.UpdateNotificationRuleRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationRuleResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationWebMapper {

    public NotificationRuleUseCase.CreateRuleCommand toCommand(CreateNotificationRuleRequest request) {
        return new NotificationRuleUseCase.CreateRuleCommand(
            request.name(),
            request.description(),
            request.eventType(),
            request.channel(),
            request.recipientType(),
            request.recipientValue(),
            request.enabled() != null ? request.enabled() : true,
            request.severityThreshold()
        );
    }

    public NotificationRuleUseCase.UpdateRuleCommand toCommand(UpdateNotificationRuleRequest request) {
        return new NotificationRuleUseCase.UpdateRuleCommand(
            request.name(),
            request.description(),
            request.eventType(),
            request.channel(),
            request.recipientType(),
            request.recipientValue(),
            request.enabled() != null ? request.enabled() : true,
            request.severityThreshold()
        );
    }

    public NotificationRuleResponse toResponse(NotificationRule rule) {
        return new NotificationRuleResponse(
            rule.id(),
            rule.name(),
            rule.description(),
            rule.eventType(),
            rule.channel(),
            rule.recipientType(),
            rule.recipientValue(),
            rule.enabled(),
            rule.severityThreshold(),
            rule.createdAt(),
            rule.updatedAt()
        );
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
            notification.id(),
            notification.ruleId(),
            notification.eventId(),
            notification.eventType(),
            notification.channel(),
            notification.recipient(),
            notification.severity(),
            notification.title(),
            notification.message(),
            notification.status(),
            notification.createdAt(),
            notification.sentAt(),
            notification.readAt(),
            notification.failureReason(),
            notification.relatedRoute()
        );
    }
}
