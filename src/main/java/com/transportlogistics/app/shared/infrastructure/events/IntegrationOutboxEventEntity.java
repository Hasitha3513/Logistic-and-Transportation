package com.transportlogistics.app.shared.infrastructure.events;

import com.transportlogistics.app.shared.DurableEventEnvelope;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_outbox_event")
class IntegrationOutboxEventEntity extends TenantScopedEntity {
    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, updatable = false, length = 100)
    private String consumerName;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false)
    private int eventVersion;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IntegrationOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected IntegrationOutboxEventEntity() {}

    static IntegrationOutboxEventEntity pending(DurableEventEnvelope event, String payloadJson,
                                                 OffsetDateTime createdAt) {
        var entity = new IntegrationOutboxEventEntity();
        entity.id = UUID.randomUUID();
        entity.eventId = event.eventId();
        entity.tenantId = event.tenantId();
        entity.consumerName = event.durableConsumer();
        entity.eventType = event.eventType();
        entity.eventVersion = event.version();
        entity.aggregateType = event.aggregateType();
        entity.aggregateId = event.aggregateId();
        entity.payload = payloadJson;
        entity.occurredAt = event.occurredAt();
        entity.status = IntegrationOutboxStatus.PENDING;
        entity.nextAttemptAt = createdAt;
        entity.createdAt = createdAt;
        entity.updatedAt = createdAt;
        return entity;
    }

    void claim(OffsetDateTime now, OffsetDateTime lockExpiry) {
        status = IntegrationOutboxStatus.PROCESSING;
        attemptCount++;
        lockedUntil = lockExpiry;
        updatedAt = now;
        lastErrorCode = null;
    }

    void publish(OffsetDateTime now) {
        status = IntegrationOutboxStatus.PUBLISHED;
        publishedAt = now;
        lockedUntil = null;
        updatedAt = now;
        lastErrorCode = null;
    }

    void retry(OffsetDateTime now, OffsetDateTime nextAttempt, String errorCode) {
        status = IntegrationOutboxStatus.RETRY;
        nextAttemptAt = nextAttempt;
        lockedUntil = null;
        updatedAt = now;
        lastErrorCode = errorCode;
    }

    void fail(OffsetDateTime now, String errorCode, boolean unsupported) {
        status = unsupported ? IntegrationOutboxStatus.UNSUPPORTED : IntegrationOutboxStatus.FAILED;
        lockedUntil = null;
        updatedAt = now;
        lastErrorCode = errorCode;
    }

    UUID id() { return id; }
    UUID eventId() { return eventId; }
    UUID tenantId() { return tenantId; }
    String consumerName() { return consumerName; }
    String eventType() { return eventType; }
    int eventVersion() { return eventVersion; }
    String aggregateType() { return aggregateType; }
    UUID aggregateId() { return aggregateId; }
    String payload() { return payload; }
    OffsetDateTime occurredAt() { return occurredAt; }
    int attemptCount() { return attemptCount; }
}
