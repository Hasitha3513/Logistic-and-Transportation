package com.transportlogistics.app.operations.ports.outbound;

import com.transportlogistics.app.operations.domain.model.RootCauseAnalysis;

import java.util.Optional;
import java.util.UUID;

public interface RootCauseAnalysisRepository {
    RootCauseAnalysis save(RootCauseAnalysis rca);
    Optional<RootCauseAnalysis> findRcaByCase(UUID tenantId, UUID caseId);
}
