package com.transportlogistics.app.notification.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleUseCase;
import com.transportlogistics.app.notification.domain.model.Notification;
import com.transportlogistics.app.notification.domain.model.NotificationRule;
import com.transportlogistics.app.notification.domain.model.NotificationEventDefinition;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.CreateNotificationRuleRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.request.UpdateNotificationRuleRequest;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationRuleResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationEventCatalogueResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationTemplateResponse;
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
            request.templateCode(),
            request.quietHoursEnabled(), request.quietStartTime(), request.quietEndTime(),
            request.quietDays(), request.suppressionWindowMinutes(),
            request.escalationEnabled(), request.escalationDelayMinutes(), request.escalationRecipientType(),
            request.escalationRecipientValue(),
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
            request.templateCode(),
            request.quietHoursEnabled(), request.quietStartTime(), request.quietEndTime(),
            request.quietDays(), request.suppressionWindowMinutes(),
            request.escalationEnabled(), request.escalationDelayMinutes(), request.escalationRecipientType(),
            request.escalationRecipientValue(),
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
            rule.templateCode(),
            rule.policy().quietHoursEnabled(), rule.policy().quietStartTime(), rule.policy().quietEndTime(),
            rule.policy().quietDays(), rule.policy().suppressionWindowMinutes(),
            rule.policy().escalationEnabled(), rule.policy().escalationDelayMinutes(),
            rule.policy().escalationRecipientType(), rule.policy().escalationRecipientValue(),
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
            notification.templateId(),
            notification.templateVersion(),
            notification.status(),
            notification.nextDeliveryAt(),
            notification.createdAt(),
            notification.sentAt(),
            notification.readAt(),
            notification.failureReason(),
            notification.relatedRoute(),
            null,
            notification.status() == com.transportlogistics.app.notification.domain.model.NotificationStatus.FAILED,
            notification.parentNotificationId(),
            notification.escalationLevel()
        );
    }

    public NotificationEventCatalogueResponse toResponse(NotificationEventDefinition definition) {
        return new NotificationEventCatalogueResponse(
            definition.eventType(), definition.owningModule(), definition.defaultSeverity(),
            definition.supportedChannels(), definition.templateCodes(),
            definition.requiredVariables(), definition.optionalVariables());
    }

    public NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
            template.id(), template.code(), template.name(), template.eventType(), template.channel(),
            template.subject(), template.body(), template.version(), template.active(),
            template.createdAt(), template.updatedAt());
    }
}
