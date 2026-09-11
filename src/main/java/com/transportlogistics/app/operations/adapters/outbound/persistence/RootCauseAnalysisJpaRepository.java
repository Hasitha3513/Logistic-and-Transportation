package com.transportlogistics.app.operations.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface RootCauseAnalysisJpaRepository extends JpaRepository<RootCauseAnalysisEntity, UUID> {
    Optional<RootCauseAnalysisEntity> findByTenantIdAndCaseId(UUID tenantId, UUID caseId);
}
