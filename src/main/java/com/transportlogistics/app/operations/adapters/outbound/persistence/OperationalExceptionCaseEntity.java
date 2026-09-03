package com.transportlogistics.app.operations.adapters.outbound.persistence;

import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "operational_exception_case")
@Getter
@Setter
class OperationalExceptionCaseEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "case_reference", nullable = false, length = 16) private String caseReference;
    @Column(name = "source_event_id", nullable = false) private UUID sourceEventId;
    @Enumerated(EnumType.STRING) @Column(name = "source_module", nullable = false, length = 24)
    private OperationalExceptionCase.SourceModule sourceModule;
    @Column(name = "source_type", nullable = false, length = 80) private String sourceType;
    @Column(name = "source_id", nullable = false) private UUID sourceId;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
    @Column(name = "summary_code", nullable = false, length = 80) private String summaryCode;
    @Column(name = "correlation_id", length = 128) private String correlationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private OperationalExceptionCase.Category category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private OperationalExceptionCase.Severity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private OperationalExceptionCase.Status status;
    @Column(name = "response_due_at", nullable = false) private OffsetDateTime responseDueAt;
    @Column(name = "resolution_due_at", nullable = false) private OffsetDateTime resolutionDueAt;
    @Column(name = "next_escalation_at") private OffsetDateTime nextEscalationAt;
    @Column(name = "acknowledged_at") private OffsetDateTime acknowledgedAt;
    @Column(name = "resolved_at") private OffsetDateTime resolvedAt;
    @Column(name = "closed_at") private OffsetDateTime closedAt;
    @Enumerated(EnumType.STRING) @Column(name = "assignment_type", length = 24)
    private OperationalExceptionCase.AssignmentType assignmentType;
    @Column(name = "assigned_user_id") private UUID assignedUserId;
    @Column(name = "assigned_role_code", length = 80) private String assignedRoleCode;
    @Enumerated(EnumType.STRING) @Column(name = "escalation_level", nullable = false, length = 8)
    private OperationalExceptionCase.EscalationLevel escalationLevel;
    @Column(name = "resolution_note", length = 2000) private String resolutionNote;
    @Column(name = "resolution_result_reference", length = 160) private String resolutionResultReference;
    @Column(name = "resolved_by") private UUID resolvedBy;
    @Column(name = "closed_by") private UUID closedBy;
    @Column(name = "resolution_validated", nullable = false) private boolean resolutionValidated;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
}
