package com.transportlogistics.app.integration.adapters.outbound.persistence;

import com.transportlogistics.app.integration.domain.model.IntegrationAuditEvent;
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
@Table(name = "integration_audit_event")
@Getter
@Setter
class IntegrationAuditEventEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(nullable = false, updatable = false, length = 255) private String actor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 64)
    private IntegrationAuditEvent.Action action;
    @Column(name = "target_type", nullable = false, updatable = false, length = 64) private String targetType;
    @Column(name = "target_id", nullable = false, updatable = false) private UUID targetId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 24)
    private IntegrationAuditEvent.Outcome outcome;
    @Column(name = "safe_code", updatable = false, length = 80) private String safeCode;
    @Column(name = "before_hash", updatable = false, length = 64) private String beforeHash;
    @Column(name = "after_hash", updatable = false, length = 64) private String afterHash;
    @Column(name = "correlation_id", updatable = false, length = 160) private String correlationId;
    @Column(name = "occurred_at", nullable = false, updatable = false) private OffsetDateTime occurredAt;
}
