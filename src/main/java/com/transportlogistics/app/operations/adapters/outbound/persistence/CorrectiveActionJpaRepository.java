package com.transportlogistics.app.operations.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CorrectiveActionJpaRepository extends JpaRepository<CorrectiveActionEntity, UUID> {
    Optional<CorrectiveActionEntity> findByTenantIdAndCaseIdAndId(UUID tenantId, UUID caseId, UUID id);
    List<CorrectiveActionEntity> findByTenantIdAndCaseIdOrderByCreatedAt(UUID tenantId, UUID caseId);
}
