package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.geofence.GeofenceEvaluationJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeofenceEvaluationJobRepositoryPort {
    GeofenceEvaluationJob enqueue(UUID tenantId, UUID positionId, Instant now);

    Optional<GeofenceEvaluationJob> find(UUID tenantId, UUID positionId);

    List<GeofenceEvaluationJob> claimDue(String leaseOwner, Instant now,
                                         Instant leaseUntil, int limit);

    boolean renew(UUID tenantId, UUID positionId, String leaseOwner, Instant now,
                  Instant leaseUntil);

    boolean release(UUID tenantId, UUID positionId, String leaseOwner, Instant now);

    void complete(UUID tenantId, UUID positionId, String leaseOwner, Instant completedAt);

    void retry(UUID tenantId, UUID positionId, String leaseOwner, Instant nextAttemptAt);
}
