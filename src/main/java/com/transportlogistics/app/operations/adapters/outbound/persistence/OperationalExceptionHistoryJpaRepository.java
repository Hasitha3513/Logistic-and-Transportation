package com.transportlogistics.app.operations.adapters.outbound.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OperationalExceptionHistoryJpaRepository extends JpaRepository<OperationalExceptionHistoryEntity, UUID> {
    Page<OperationalExceptionHistoryEntity> findByTenantIdAndCaseId(UUID tenantId, UUID caseId, Pageable pageable);
}
