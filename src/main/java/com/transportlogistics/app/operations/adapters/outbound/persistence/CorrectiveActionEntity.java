package com.transportlogistics.app.operations.adapters.outbound.persistence;

import com.transportlogistics.app.operations.domain.model.CorrectiveAction;
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
@Table(name = "operational_exception_corrective_action")
@Getter
@Setter
class CorrectiveActionEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "case_id", nullable = false) private UUID caseId;
    @Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false, length = 24) private CorrectiveAction.Type type;
    @Column(nullable = false, length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "owner_type", nullable = false, length = 24)
    private OperationalExceptionCase.AssignmentType ownerType;
    @Column(name = "owner_user_id") private UUID ownerUserId;
    @Column(name = "owner_role_code", length = 80) private String ownerRoleCode;
    @Column(name = "due_at") private OffsetDateTime dueAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private CorrectiveAction.Status status;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Column(name = "evidence_reference", length = 160) private String evidenceReference;
    @Column(name = "cancellation_reason", length = 2000) private String cancellationReason;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
}
