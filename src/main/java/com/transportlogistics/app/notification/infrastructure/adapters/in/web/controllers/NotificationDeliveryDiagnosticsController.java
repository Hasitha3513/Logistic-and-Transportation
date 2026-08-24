package com.transportlogistics.app.notification.infrastructure.adapters.in.web.controllers;

import com.transportlogistics.app.notification.application.ports.in.NotificationDeliveryDiagnosticsUseCase;
import com.transportlogistics.app.notification.domain.model.NotificationStatus;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationDeliveryAttemptResponse;
import com.transportlogistics.app.notification.infrastructure.adapters.in.web.dto.response.NotificationDeliveryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification-deliveries")
public class NotificationDeliveryDiagnosticsController {
    private final NotificationDeliveryDiagnosticsUseCase useCase;

    public NotificationDeliveryDiagnosticsController(NotificationDeliveryDiagnosticsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public List<NotificationDeliveryResponse> find(
        @RequestParam(required = false) NotificationStatus status,
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
        @RequestParam(defaultValue = "100") int limit) {
        return useCase.find(status, eventType, from, to, limit).stream().map(diagnostic -> {
            var n = diagnostic.notification();
            return new NotificationDeliveryResponse(n.id(), n.ruleId(), n.eventId(), n.eventType(), n.channel(),
                n.status(), diagnostic.attemptCount(), n.nextDeliveryAt(), n.status() == NotificationStatus.FAILED,
                n.parentNotificationId(), n.escalationLevel(), n.createdAt(), n.sentAt(), mask(n.recipient()));
        }).toList();
    }

    @GetMapping("/{id}/attempts")
    public ResponseEntity<List<NotificationDeliveryAttemptResponse>> attempts(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.attempts(id).stream().map(a -> new NotificationDeliveryAttemptResponse(
            a.id(), a.attemptNumber(), a.state(), a.dueAt(), a.startedAt(), a.completedAt(), a.errorCategory(),
            a.errorCode(), a.errorMessage(), a.providerMessageId())).toList());
    }

    private static String mask(String recipient) {
        if (recipient == null || recipient.isBlank()) return null;
        int at = recipient.indexOf('@');
        if (at > 0) return recipient.substring(0, 1) + "***" + recipient.substring(at);
        return recipient.substring(0, 1) + "***";
    }
}
