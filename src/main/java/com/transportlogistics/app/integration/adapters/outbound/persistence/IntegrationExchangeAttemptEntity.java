package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.transportlogistics.app.integration.domain.model.IntegrationExchangeAttempt;
import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_exchange_attempt")
@Getter
@Setter
class IntegrationExchangeAttemptEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "exchange_id", nullable = false, updatable = false) private UUID exchangeId;
    @Column(name = "attempt_number", nullable = false, updatable = false) private int attemptNumber;
    @Column(name = "started_at", nullable = false, updatable = false) private OffsetDateTime startedAt;
    @Column(name = "completed_at", nullable = false, updatable = false) private OffsetDateTime completedAt;
    @Column(name = "latency_ms", nullable = false, updatable = false) private long latencyMillis;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 24)
    private IntegrationExchangeAttempt.Outcome outcome;
    @Column(name = "error_code", updatable = false, length = 80) private String errorCode;
    @Column(name = "external_correlation_id", updatable = false, length = 160) private String externalCorrelationId;
    @Column(name = "target_filename", updatable = false, length = 80) private String targetFilename;
}
