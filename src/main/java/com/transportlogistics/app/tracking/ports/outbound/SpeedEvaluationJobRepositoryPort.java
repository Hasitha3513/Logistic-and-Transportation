package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeedEvaluationJobRepositoryPort {
    SpeedEvaluationJob enqueue(UUID tenantId, UUID positionId, Instant now);
    Optional<SpeedEvaluationJob> find(UUID tenantId, UUID positionId);
    List<SpeedEvaluationJob> claimDue(String leaseOwner, Instant now, Instant leaseUntil, int limit);
    void complete(UUID tenantId, UUID positionId, String leaseOwner, Instant completedAt);
    void retry(UUID tenantId, UUID positionId, String leaseOwner, Instant now, Instant nextAttemptAt);
    void fail(UUID tenantId, UUID positionId, String leaseOwner, Instant failedAt);
}
