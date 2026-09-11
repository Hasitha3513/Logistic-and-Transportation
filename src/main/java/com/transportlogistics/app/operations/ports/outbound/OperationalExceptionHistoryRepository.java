package com.transportlogistics.app.operations.ports.outbound;

import com.transportlogistics.app.operations.domain.model.AssignmentHistory;
import com.transportlogistics.app.operations.domain.model.OperationalExceptionHistory;
import com.transportlogistics.app.operations.ports.inbound.OperationalExceptionUseCase;

import java.util.UUID;

public interface OperationalExceptionHistoryRepository {
    void append(OperationalExceptionHistory event);
    void appendAssignment(AssignmentHistory event);
    OperationalExceptionUseCase.PageResult<OperationalExceptionHistory> findByCase(
        UUID tenantId, UUID caseId, int page, int size);
}
