package com.transportlogistics.app.operations.ports.outbound;

import com.transportlogistics.app.operations.domain.model.OperationalExceptionCase;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalExceptionCaseRepository {
    OperationalExceptionCase save(OperationalExceptionCase exceptionCase);
    Optional<OperationalExceptionCase> find(UUID tenantId, UUID id);
    Optional<OperationalExceptionCase> findBySourceEvent(UUID tenantId, UUID sourceEventId);
    boolean referenceExists(UUID tenantId, String reference);
    OperationalExceptionUseCase.PageResult<OperationalExceptionCase> search(UUID tenantId,
        OperationalExceptionUseCase.Query query, OffsetDateTime now);
    List<OperationalExceptionCase> findDue(UUID tenantId, OffsetDateTime now, int limit);
}
