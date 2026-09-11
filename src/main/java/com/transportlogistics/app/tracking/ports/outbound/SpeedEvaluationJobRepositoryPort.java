package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.speed.SpeedEvaluationJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeedEvaluationJobRepositoryPort {
    SpeedEvaluationJob enqueue(UUID tenantId, UUID positionId, UUID vehicleId,
                               Instant sourceTimestamp, Instant now);
    Optional<SpeedEvaluationJob> find(UUID tenantId, UUID positionId);
    List<SpeedEvaluationJob> claimDue(String leaseOwner, Instant now, Instant leaseUntil, int limit);
    boolean renew(UUID tenantId, UUID positionId, String leaseOwner, Instant now, Instant leaseUntil);
    boolean release(UUID tenantId, UUID positionId, String leaseOwner, Instant now);
    void complete(UUID tenantId, UUID positionId, String leaseOwner, Instant completedAt);
    void retry(UUID tenantId, UUID positionId, String leaseOwner, Instant now, Instant nextAttemptAt);
    void fail(UUID tenantId, UUID positionId, String leaseOwner, String errorCode, Instant failedAt);
}
