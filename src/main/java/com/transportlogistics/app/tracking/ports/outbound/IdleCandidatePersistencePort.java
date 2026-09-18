package com.transportlogistics.app.tracking.ports.outbound;

import com.transportlogistics.app.tracking.domain.idle.IdleCandidate;
import com.transportlogistics.app.tracking.domain.idle.IdleCandidate.Evidence;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdleCandidatePersistencePort {
    Optional<IdleCandidate> find(UUID tenantId, UUID vehicleId);
    Result save(IdleCandidate candidate, Evidence evidence, long expectedVersion);
    void discard(UUID tenantId, UUID vehicleId, UUID candidateId, long expectedVersion);
    UUID promote(IdleCandidate candidate, Evidence evidence, Instant confirmedAt, long expectedVersion);
    int purgeExpired(Instant now, int limit);
    enum Result { APPLIED, DUPLICATE }
}
