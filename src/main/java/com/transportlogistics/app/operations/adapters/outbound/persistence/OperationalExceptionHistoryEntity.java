package com.transportlogistics.app.operations.adapters.outbound.persistence;

import com.transportlogistics.app.shared.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_exception_history")
@Getter
@Setter
class OperationalExceptionHistoryEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "case_id", nullable = false) private UUID caseId;
    @Column(nullable = false, length = 64) private String action;
    @Column(name = "before_value", length = 2000) private String beforeValue;
    @Column(name = "after_value", length = 2000) private String afterValue;
    @Column(length = 2000) private String reason;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Column(name = "actor_username", nullable = false, length = 128) private String actorUsername;
    @Column(name = "correlation_id", length = 128) private String correlationId;
    @Column(name = "resulting_version", nullable = false) private long resultingVersion;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
}
