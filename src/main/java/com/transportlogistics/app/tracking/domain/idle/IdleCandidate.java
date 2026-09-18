package com.transportlogistics.app.tracking.domain.idle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IdleCandidate(UUID tenantId, UUID vehicleId, UUID deviceId, UUID candidateId,
        UUID referenceHistoryId, Instant startedAt, Instant latestSourceTimestamp,
        Instant lastQualifyingAt, Instant recoveryStartedAt, long creditedSeconds,
        int evidenceCount, String lastDedupeIdentity, long version) {
    public IdleCandidate {
        Objects.requireNonNull(tenantId); Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(deviceId); Objects.requireNonNull(candidateId);
        Objects.requireNonNull(referenceHistoryId); Objects.requireNonNull(startedAt);
        Objects.requireNonNull(latestSourceTimestamp); Objects.requireNonNull(lastDedupeIdentity);
        if (creditedSeconds < 0 || evidenceCount < 1 || version < 0) {
            throw new IllegalArgumentException("Invalid idle candidate");
        }
    }

    public record Evidence(UUID id, UUID tenantId, UUID candidateId, UUID vehicleId,
            UUID deviceId, UUID historyId, Instant sourceTimestamp, String dedupeIdentity,
            String outcome, String engineRunningState, String engineRunningSource,
            BigDecimal speedKph, BigDecimal accuracyMeters, BigDecimal adjustedDistanceMeters,
            int creditedDeltaSeconds) { }
}
