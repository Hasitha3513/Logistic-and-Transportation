package com.transportlogistics.app.operations.adapters.outbound.persistence;

import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
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
@Table(name = "operational_exception_assignment_history")
@Getter
@Setter
class AssignmentHistoryEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "case_id", nullable = false) private UUID caseId;
    @Enumerated(EnumType.STRING) @Column(name = "from_type", length = 24) private OperationalExceptionCase.AssignmentType fromType;
    @Column(name = "from_user_id") private UUID fromUserId;
    @Column(name = "from_role_code", length = 80) private String fromRoleCode;
    @Enumerated(EnumType.STRING) @Column(name = "to_type", length = 24) private OperationalExceptionCase.AssignmentType toType;
    @Column(name = "to_user_id") private UUID toUserId;
    @Column(name = "to_role_code", length = 80) private String toRoleCode;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Column(name = "actor_username", nullable = false, length = 128) private String actorUsername;
    @Column(nullable = false, length = 2000) private String reason;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
}
