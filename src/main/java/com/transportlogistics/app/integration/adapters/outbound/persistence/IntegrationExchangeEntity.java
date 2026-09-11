package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.transportlogistics.app.integration.domain.model.IntegrationExchange;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_exchange")
@Getter
@Setter
class IntegrationExchangeEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "configuration_id", nullable = false, updatable = false) private UUID configurationId;
    @Column(name = "source_event_id", nullable = false, updatable = false) private UUID sourceEventId;
    @Column(name = "source_event_type", nullable = false, updatable = false, length = 100) private String sourceEventType;
    @Column(name = "mapping_version_id", nullable = false, updatable = false) private UUID mappingVersionId;
    @Column(name = "mapping_definition_hash", nullable = false, updatable = false, length = 64)
    private String mappingDefinitionHash;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canonical_payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String canonicalPayload;
    @Column(name = "payload_hash", nullable = false, updatable = false, length = 64) private String payloadHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private IntegrationExchange.Status status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "next_attempt_at", nullable = false) private OffsetDateTime nextAttemptAt;
    @Column(name = "locked_until") private OffsetDateTime lockedUntil;
    @Column(name = "external_correlation_id", length = 160) private String externalCorrelationId;
    @Column(name = "target_filename", length = 80) private String targetFilename;
    @Column(name = "last_error_code", length = 80) private String lastErrorCode;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Version @Column(nullable = false) private long version;

    void claim(OffsetDateTime now) {
        status = IntegrationExchange.Status.IN_PROGRESS;
        attemptCount++;
        lockedUntil = now.plus(IntegrationExchange.CLAIM_LEASE);
        updatedAt = now;
        lastErrorCode = null;
    }

    void exhaust(OffsetDateTime now) {
        status = IntegrationExchange.Status.FAILED;
        lockedUntil = null;
        completedAt = now;
        updatedAt = now;
        lastErrorCode = "INTEGRATION_RETRY_EXHAUSTED";
    }
}
