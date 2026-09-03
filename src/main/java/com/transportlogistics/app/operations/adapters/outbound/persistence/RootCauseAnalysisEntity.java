package com.transportlogistics.app.operations.adapters.outbound.persistence;

import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;
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
@Table(name = "operational_exception_rca")
@Getter
@Setter
class RootCauseAnalysisEntity extends TenantScopedEntity {
    @Id private UUID id;
    @Column(name = "case_id", nullable = false) private UUID caseId;
    @Enumerated(EnumType.STRING) @Column(name = "cause_category", nullable = false, length = 24)
    private RootCauseAnalysis.CauseCategory causeCategory;
    @Column(name = "root_cause_code", nullable = false, length = 80) private String rootCauseCode;
    @Column(nullable = false, length = 2000) private String summary;
    @Column(name = "contributing_factors", length = 2000) private String contributingFactors;
    @Column(name = "author_id", nullable = false) private UUID authorId;
    @Column(name = "approver_id") private UUID approverId;
    @Column(name = "approved_at") private OffsetDateTime approvedAt;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
}
