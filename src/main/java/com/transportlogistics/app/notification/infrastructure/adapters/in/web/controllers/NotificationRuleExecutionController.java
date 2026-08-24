package com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.notification.application.ports.in.NotificationRuleExecutionUseCase;
import com.transportlogistics.app.notification.domain.model.NotificationRuleExecution;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationRuleExecutionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class NotificationRuleExecutionController {
    private final NotificationRuleExecutionUseCase useCase;

    public NotificationRuleExecutionController(NotificationRuleExecutionUseCase useCase) { this.useCase = useCase; }

    @GetMapping("/notification-rule-executions")
    public List<NotificationRuleExecutionResponse> list(@RequestParam(required = false) UUID ruleId,
                                                        @RequestParam(required = false) UUID eventId,
                                                        @RequestParam(defaultValue = "100") int limit) {
        return useCase.listExecutions(ruleId, eventId, limit).stream().map(this::response).toList();
    }

    private NotificationRuleExecutionResponse response(NotificationRuleExecution value) {
        return new NotificationRuleExecutionResponse(value.id(), value.eventId(), value.eventType(),
            value.aggregateType(), value.aggregateId(), value.ruleId(), mask(value.resolvedRecipient()),
            value.channel(), value.outcome(), value.controllingNotificationId(), value.failureCode(),
            value.failureMessage(), value.createdAt(), value.completedAt());
    }

    private String mask(String recipient) {
        if (recipient == null || recipient.isBlank()) return null;
        int at = recipient.indexOf('@');
        if (at > 0) return recipient.charAt(0) + "***" + recipient.substring(at);
        return recipient.length() < 3 ? "***" : recipient.substring(0, 2) + "***";
    }
}
