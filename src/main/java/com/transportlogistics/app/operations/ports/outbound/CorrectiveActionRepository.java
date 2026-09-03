package com.transportlogistics.app.operations.ports.outbound;

import com.transportlogistics.app.operations.domain.model.CorrectiveAction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorrectiveActionRepository {
    CorrectiveAction save(CorrectiveAction action);
    Optional<CorrectiveAction> find(UUID tenantId, UUID caseId, UUID actionId);
    List<CorrectiveAction> findActionsByCase(UUID tenantId, UUID caseId);
}
