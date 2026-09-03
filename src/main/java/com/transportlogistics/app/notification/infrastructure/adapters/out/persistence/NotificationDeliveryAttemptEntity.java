package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;
import com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttempt;
import com.transportlogistics.app.notification.domain.model.NotificationDeliveryAttemptState;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_delivery_attempt")
public class NotificationDeliveryAttemptEntity extends com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "notification_id", nullable = false) private UUID notificationId;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private NotificationDeliveryAttemptState state;
    @Column(name = "due_at", nullable = false) private OffsetDateTime dueAt;
    @Column(name = "started_at") private OffsetDateTime startedAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Enumerated(EnumType.STRING) @Column(name = "error_category", length = 32) private EmailDeliveryErrorCategory errorCategory;
    @Column(name = "error_code", length = 64) private String errorCode;
    @Column(name = "error_message", length = 500) private String errorMessage;
    @Column(name = "provider_message_id", length = 255) private String providerMessageId;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    protected NotificationDeliveryAttemptEntity() {}

    static NotificationDeliveryAttemptEntity fromDomain(NotificationDeliveryAttempt attempt) {
        var entity = new NotificationDeliveryAttemptEntity();
        entity.id = attempt.id(); entity.notificationId = attempt.notificationId();
        entity.attemptNumber = attempt.attemptNumber(); entity.state = attempt.state(); entity.dueAt = attempt.dueAt();
        entity.startedAt = attempt.startedAt(); entity.completedAt = attempt.completedAt();
        entity.errorCategory = attempt.errorCategory(); entity.errorCode = attempt.errorCode();
        entity.errorMessage = attempt.errorMessage(); entity.providerMessageId = attempt.providerMessageId();
        entity.createdAt = attempt.createdAt();
        return entity;
    }

    NotificationDeliveryAttempt toDomain() {
        return new NotificationDeliveryAttempt(id, notificationId, attemptNumber, state, dueAt, startedAt,
            completedAt, errorCategory, errorCode, errorMessage, providerMessageId, createdAt);
    }
}
