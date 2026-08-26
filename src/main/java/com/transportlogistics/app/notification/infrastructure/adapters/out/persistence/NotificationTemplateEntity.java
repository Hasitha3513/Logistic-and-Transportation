package com.transportlogistics.app.notification.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.notification.domain.model.NotificationChannel;
import com.transportlogistics.app.notification.domain.model.NotificationTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_template")
public class NotificationTemplateEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 64)
    private String code;
    @Column(nullable = false, length = 128)
    private String name;
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;
    @Column(nullable = false, length = 255)
    private String subject;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    @Column(nullable = false)
    private int version;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public NotificationTemplateEntity() {
    }

    public NotificationTemplate toDomain() {
        return new NotificationTemplate(id, code, name, eventType, channel, subject, body,
            version, active, createdAt, updatedAt);
    }
}
