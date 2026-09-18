package com.transportlogistics.app.tracking.domain.idle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class IdlePersistenceModels {
    private IdlePersistenceModels() { }

    public enum StateValue { UNKNOWN, UNSUPPORTED, NOT_REPORTED, STALE,
        CONFLICTING_EVIDENCE, CANDIDATE, IDLE, NORMAL }
    public enum CapabilityState { SUPPORTED, UNSUPPORTED, UNKNOWN }
    public enum EpisodeLifecycle { CANDIDATE, CONFIRMED, CLOSED }
    public enum EndReason { ENGINE_STOPPED, MOVEMENT, EVIDENCE_GAP,
        DEVICE_REASSIGNED, CAPABILITY_CHANGED }
    public enum EvidenceOutcome { QUALIFYING, NOT_QUALIFYING, UNKNOWN, CONFLICTING_EVIDENCE }

    public record State(UUID tenantId, UUID vehicleId, UUID deviceId, StateValue state,
            CapabilityState capabilityState, Instant latestSourceTimestamp,
            Instant candidateStartedAt, Instant lastQualifyingAt, long creditedSeconds,
            int evidenceCount, UUID openEpisodeId, String lastDedupeIdentity, long version) {
        public State {
            Objects.requireNonNull(tenantId); Objects.requireNonNull(vehicleId);
            Objects.requireNonNull(deviceId); Objects.requireNonNull(state);
            Objects.requireNonNull(capabilityState); Objects.requireNonNull(latestSourceTimestamp);
            if (creditedSeconds < 0 || evidenceCount < 0 || version < 0
                    || lastDedupeIdentity == null || !lastDedupeIdentity.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid idle state");
            }
        }
    }

    public record Episode(UUID id, UUID tenantId, UUID vehicleId, UUID deviceId,
            EpisodeLifecycle lifecycle, Instant startSourceTimestamp, Instant confirmedAt,
            Instant lastSourceTimestamp, Instant endSourceTimestamp, EndReason endReason,
            long creditedSeconds, int evidenceCount, long version) {
        public Episode {
            Objects.requireNonNull(id);
            Objects.requireNonNull(tenantId);
            Objects.requireNonNull(vehicleId);
            Objects.requireNonNull(deviceId);
            Objects.requireNonNull(lifecycle);
            Objects.requireNonNull(startSourceTimestamp);
            Objects.requireNonNull(lastSourceTimestamp);
            if (lastSourceTimestamp.isBefore(startSourceTimestamp) || creditedSeconds < 0
                    || evidenceCount < 0 || version < 0) {
                throw new IllegalArgumentException("Invalid idle episode");
            }
            boolean closed = lifecycle == EpisodeLifecycle.CLOSED;
            if (closed != (endSourceTimestamp != null && endReason != null)
                    || lifecycle == EpisodeLifecycle.CANDIDATE && confirmedAt != null
                    || lifecycle == EpisodeLifecycle.CONFIRMED && confirmedAt == null) {
                throw new IllegalArgumentException("Invalid idle episode lifecycle");
            }
        }
    }

    public record Evidence(UUID id, UUID tenantId, UUID episodeId, UUID vehicleId, UUID deviceId,
            UUID historyId, Instant sourceTimestamp, String dedupeIdentity, EvidenceOutcome outcome,
            String engineRunningState, String engineRunningSource, BigDecimal speedKph,
            BigDecimal horizontalAccuracyMeters, BigDecimal adjustedDistanceMeters,
            int creditedDeltaSeconds) {
        public Evidence {
            Objects.requireNonNull(id);
            Objects.requireNonNull(tenantId);
            Objects.requireNonNull(episodeId);
            Objects.requireNonNull(vehicleId);
            Objects.requireNonNull(deviceId);
            Objects.requireNonNull(historyId);
            Objects.requireNonNull(sourceTimestamp);
            Objects.requireNonNull(outcome);
            if (dedupeIdentity == null || !dedupeIdentity.matches("[0-9a-f]{64}")
                    || creditedDeltaSeconds < 0 || creditedDeltaSeconds > 120) {
                throw new IllegalArgumentException("Invalid idle evidence");
            }
        }
    }

    public record Mutation(State state, Episode episode, Evidence evidence,
            long expectedStateVersion, long expectedEpisodeVersion, boolean createEpisode) {
        public Mutation {
            Objects.requireNonNull(state);
            Objects.requireNonNull(episode);
            Objects.requireNonNull(evidence);
            if (!state.tenantId().equals(episode.tenantId())
                    || !state.tenantId().equals(evidence.tenantId())
                    || !state.vehicleId().equals(episode.vehicleId())
                    || !state.vehicleId().equals(evidence.vehicleId())
                    || !state.deviceId().equals(episode.deviceId())
                    || !state.deviceId().equals(evidence.deviceId())
                    || !episode.id().equals(evidence.episodeId())) {
                throw new IllegalArgumentException("Idle mutation identity mismatch");
            }
        }
    }
    public enum PersistResult { APPLIED, DUPLICATE }
}
