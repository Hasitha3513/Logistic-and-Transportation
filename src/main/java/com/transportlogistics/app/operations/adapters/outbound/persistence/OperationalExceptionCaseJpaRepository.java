package com.transportlogistics.app.operations.adapters.outbound.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OperationalExceptionCaseJpaRepository extends JpaRepository<OperationalExceptionCaseEntity, UUID>,
        JpaSpecificationExecutor<OperationalExceptionCaseEntity> {
    Optional<OperationalExceptionCaseEntity> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<OperationalExceptionCaseEntity> findByTenantIdAndSourceEventId(UUID tenantId, UUID sourceEventId);
    boolean existsByTenantIdAndCaseReference(UUID tenantId, String caseReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OperationalExceptionCaseEntity c where c.tenantId = :tenantId "
        + "and c.status not in ('RESOLVED','CLOSED') and c.nextEscalationAt is not null "
        + "and c.nextEscalationAt <= :now order by c.nextEscalationAt, c.id")
    List<OperationalExceptionCaseEntity> findDue(UUID tenantId, OffsetDateTime now, Pageable pageable);
}
