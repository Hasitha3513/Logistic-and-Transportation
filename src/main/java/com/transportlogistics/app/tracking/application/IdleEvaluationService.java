package com.transportlogistics.app.tracking.application;

import com.transportlogistics.app.fleet.VehiclePowertrainEligibilityQuery;
import com.transportlogistics.app.tracking.application.provider.TelemetryCapabilityState;
import com.transportlogistics.app.tracking.application.provider.TelemetrySignalCapability;
import com.transportlogistics.app.tracking.application.telemetry.HistoricalTelemetry;
import com.transportlogistics.app.tracking.application.telemetry.TrackingTelemetryIngestedV3.EngineRunningState;
import com.transportlogistics.app.tracking.domain.TrackingModels.Ordering;
import com.transportlogistics.app.tracking.domain.TrackingModels.Trust;
import com.transportlogistics.app.tracking.domain.idle.IdleCandidate;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.EndReason;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Episode;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.EpisodeLifecycle;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.EvidenceOutcome;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.Mutation;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.State;
import com.transportlogistics.app.tracking.domain.idle.IdlePersistenceModels.StateValue;
import com.transportlogistics.app.tracking.ports.inbound.IdleEvaluationUseCase;
import com.transportlogistics.app.tracking.ports.outbound.HistoricalTelemetryLookupPort;
import com.transportlogistics.app.tracking.ports.outbound.IdleCandidatePersistencePort;
import com.transportlogistics.app.tracking.ports.outbound.IdlePersistencePort;
import com.transportlogistics.app.tracking.ports.outbound.TelemetryCapabilityLookupPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Approved US-51 source-time idle state machine. */
public final class IdleEvaluationService implements IdleEvaluationUseCase {
    private static final BigDecimal MAX_SPEED = new BigDecimal("3.0");
    private static final BigDecimal MAX_ACCURACY = new BigDecimal("100");
    private static final BigDecimal MAX_DISTANCE = new BigDecimal("50");
    private static final long MAX_GAP_SECONDS = 120;
    private static final long CONFIRM_SECONDS = 300;
    private final VehiclePowertrainEligibilityQuery powertrains;
    private final TelemetryCapabilityLookupPort capabilities;
    private final HistoricalTelemetryLookupPort history;
    private final IdleCandidatePersistencePort candidates;
    private final IdlePersistencePort idle;

    public IdleEvaluationService(VehiclePowertrainEligibilityQuery powertrains,
            TelemetryCapabilityLookupPort capabilities, HistoricalTelemetryLookupPort history,
            IdleCandidatePersistencePort candidates, IdlePersistencePort idle) {
        this.powertrains = powertrains;
        this.capabilities = capabilities;
        this.history = history;
        this.candidates = candidates;
        this.idle = idle;
    }

    @Override
    public void evaluate(HistoricalTelemetry fact, Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt, "evaluatedAt");
        if (fact.eventVersion() != 3 || fact.trust() != Trust.TRUSTED
                || fact.ordering() != Ordering.IN_ORDER) {
            discardCandidate(fact);
            return;
        }
        State state = idle.findState(fact.tenantId(), fact.vehicleId()).orElse(null);
        if (state != null && state.state() == StateValue.IDLE) {
            evaluateConfirmed(fact, state);
        } else {
            evaluateCandidate(fact);
        }
    }

    private void evaluateCandidate(HistoricalTelemetry fact) {
        IdleCandidate current = candidates.find(fact.tenantId(), fact.vehicleId()).orElse(null);
        if (current != null && !fact.deviceId().equals(current.deviceId())) {
            candidates.discard(current.tenantId(), current.vehicleId(), current.candidateId(),
                    current.version());
            return;
        }
        if (!eligible(fact) || fact.engineRunningState() != EngineRunningState.RUNNING) {
            if (current != null) candidates.discard(current.tenantId(), current.vehicleId(),
                    current.candidateId(), current.version());
            return;
        }
        HistoricalTelemetry reference = current == null ? fact : reference(current);
        Assessment assessment = assess(fact, reference);
        if (!assessment.qualifying()) {
            if (current != null) candidates.discard(current.tenantId(), current.vehicleId(),
                    current.candidateId(), current.version());
            return;
        }
        if (current == null) {
            UUID candidateId = UUID.randomUUID();
            IdleCandidate created = new IdleCandidate(fact.tenantId(), fact.vehicleId(), fact.deviceId(),
                    candidateId, fact.eventId(), fact.recordedAt(), fact.recordedAt(), fact.recordedAt(),
                    null, 0, 1, fact.dedupeIdentity(), 0);
            candidates.save(created, candidateEvidence(created, fact, assessment, 0), -1);
            return;
        }
        if (!fact.recordedAt().isAfter(current.latestSourceTimestamp())) {
            handleCandidateReplayOrConflict(current, fact, assessment);
            return;
        }
        long delta = Duration.between(current.lastQualifyingAt(), fact.recordedAt()).getSeconds();
        if (delta > MAX_GAP_SECONDS) {
            candidates.discard(current.tenantId(), current.vehicleId(), current.candidateId(), current.version());
            return;
        }
        IdleCandidate next = new IdleCandidate(current.tenantId(), current.vehicleId(), fact.deviceId(),
                current.candidateId(), fact.eventId(), current.startedAt(), fact.recordedAt(), fact.recordedAt(),
                null, current.creditedSeconds() + delta, current.evidenceCount() + 1,
                fact.dedupeIdentity(), current.version() + 1);
        IdleCandidate.Evidence evidence = candidateEvidence(next, fact, assessment, (int) delta);
        if (next.creditedSeconds() >= CONFIRM_SECONDS && next.evidenceCount() >= 2) {
            candidates.promote(next, evidence, fact.recordedAt(), current.version());
        } else {
            candidates.save(next, evidence, current.version());
        }
    }

    private void evaluateConfirmed(HistoricalTelemetry fact, State state) {
        Episode episode = idle.findEpisode(fact.tenantId(), state.openEpisodeId()).orElseThrow();
        if (fact.recordedAt().isBefore(state.latestSourceTimestamp())) return;
        if (fact.recordedAt().equals(state.latestSourceTimestamp())) {
            if (!fact.dedupeIdentity().equals(state.lastDedupeIdentity())) {
                close(fact, state, episode, EndReason.EVIDENCE_GAP,
                        state.lastQualifyingAt(), EvidenceOutcome.CONFLICTING_EVIDENCE, 0, null);
            }
            return;
        }
        if (!eligible(fact) || !fact.deviceId().equals(state.deviceId())) {
            close(fact, state, episode, fact.deviceId().equals(state.deviceId())
                    ? EndReason.CAPABILITY_CHANGED : EndReason.DEVICE_REASSIGNED,
                    fact.recordedAt(), EvidenceOutcome.UNKNOWN, 0, null);
            return;
        }
        if (fact.engineRunningState() == EngineRunningState.NOT_RUNNING) {
            close(fact, state, episode, EndReason.ENGINE_STOPPED, fact.recordedAt(),
                    EvidenceOutcome.NOT_QUALIFYING, 0, null);
            return;
        }
        HistoricalTelemetry reference = history.findExact(fact.tenantId(), state.lastQualifyingAt(),
                state.referenceHistoryId()).orElseThrow();
        Assessment assessment = assess(fact, reference);
        long gap = Duration.between(state.lastQualifyingAt(), fact.recordedAt()).getSeconds();
        if (fact.engineRunningState() != EngineRunningState.RUNNING || gap > MAX_GAP_SECONDS) {
            close(fact, state, episode, EndReason.EVIDENCE_GAP, state.lastQualifyingAt(),
                    EvidenceOutcome.UNKNOWN, 0, assessment);
        } else if (assessment.qualifying()) {
            updateOpen(fact, state, episode, assessment, (int) gap, null);
        } else if (state.recoveryStartedAt() == null) {
            updateOpen(fact, state, episode, assessment, 0, fact.recordedAt());
        } else if (Duration.between(state.recoveryStartedAt(), fact.recordedAt()).getSeconds() >= 30) {
            close(fact, state, episode, EndReason.MOVEMENT, state.recoveryStartedAt(),
                    EvidenceOutcome.NOT_QUALIFYING, 0, assessment);
        } else {
            updateOpen(fact, state, episode, assessment, 0, state.recoveryStartedAt());
        }
    }

    private void updateOpen(HistoricalTelemetry fact, State previous, Episode episode,
            Assessment assessment, int credited, Instant recoveryStartedAt) {
        long total = previous.creditedSeconds() + credited;
        State state = new State(previous.tenantId(), previous.vehicleId(), previous.deviceId(),
                StateValue.IDLE, IdlePersistenceModels.CapabilityState.SUPPORTED, fact.recordedAt(),
                previous.candidateStartedAt(), credited > 0 ? fact.recordedAt() : previous.lastQualifyingAt(),
                total, previous.evidenceCount() + 1, episode.id(),
                credited > 0 ? fact.eventId() : previous.referenceHistoryId(), recoveryStartedAt,
                fact.dedupeIdentity(), previous.version() + 1);
        Episode updated = new Episode(episode.id(), episode.tenantId(), episode.vehicleId(),
                episode.deviceId(), EpisodeLifecycle.CONFIRMED, episode.startSourceTimestamp(),
                episode.confirmedAt(), fact.recordedAt(), null, null, total,
                episode.evidenceCount() + 1, episode.version() + 1);
        idle.persist(new Mutation(state, updated, episodeEvidence(fact, episode.id(),
                assessment, credited, assessment.qualifying() ? EvidenceOutcome.QUALIFYING
                        : EvidenceOutcome.NOT_QUALIFYING), previous.version(), episode.version(), false));
    }

    private void close(HistoricalTelemetry fact, State previous, Episode episode, EndReason reason,
            Instant closedAt, EvidenceOutcome outcome, int credited, Assessment assessment) {
        State state = new State(previous.tenantId(), previous.vehicleId(), previous.deviceId(),
                StateValue.NORMAL, previous.capabilityState(), fact.recordedAt(), null, null,
                0, 0, null, null, null, fact.dedupeIdentity(), previous.version() + 1);
        Episode closed = new Episode(episode.id(), episode.tenantId(), episode.vehicleId(), episode.deviceId(),
                EpisodeLifecycle.CLOSED, episode.startSourceTimestamp(), episode.confirmedAt(),
                fact.recordedAt(), closedAt, reason, episode.creditedSeconds(),
                episode.evidenceCount() + 1, episode.version() + 1);
        idle.persist(new Mutation(state, closed, episodeEvidence(fact, episode.id(), assessment,
                credited, outcome), previous.version(), episode.version(), false));
    }

    private boolean eligible(HistoricalTelemetry fact) {
        var classification = powertrains.classify(fact.tenantId(), fact.vehicleId(), fact.recordedAt());
        return (classification == VehiclePowertrainEligibilityQuery.Classification.COMBUSTION
                || classification == VehiclePowertrainEligibilityQuery.Classification.HYBRID)
                && capabilities.resolve(fact.tenantId(), fact.deviceId(),
                        TelemetrySignalCapability.ENGINE_RUNNING, fact.recordedAt())
                        == TelemetryCapabilityState.SUPPORTED
                && fact.engineRunningState() != null && fact.engineRunningSource() != null;
    }

    private HistoricalTelemetry reference(IdleCandidate candidate) {
        return history.findExact(candidate.tenantId(), candidate.lastQualifyingAt(),
                candidate.referenceHistoryId()).orElseThrow();
    }

    private static Assessment assess(HistoricalTelemetry fact, HistoricalTelemetry reference) {
        if (fact.speedKph() == null || fact.horizontalAccuracyMeters() == null
                || reference.horizontalAccuracyMeters() == null
                || fact.horizontalAccuracyMeters().signum() < 0
                || reference.horizontalAccuracyMeters().signum() < 0
                || fact.horizontalAccuracyMeters().compareTo(MAX_ACCURACY) > 0
                || reference.horizontalAccuracyMeters().compareTo(MAX_ACCURACY) > 0) {
            return new Assessment(false, null);
        }
        BigDecimal adjusted = haversine(reference.latitude(), reference.longitude(),
                fact.latitude(), fact.longitude()).subtract(reference.horizontalAccuracyMeters())
                .subtract(fact.horizontalAccuracyMeters()).max(BigDecimal.ZERO)
                .setScale(3, RoundingMode.HALF_UP);
        return new Assessment(fact.speedKph().compareTo(MAX_SPEED) <= 0
                && adjusted.compareTo(MAX_DISTANCE) <= 0, adjusted);
    }

    private static BigDecimal haversine(BigDecimal lat1, BigDecimal lon1,
            BigDecimal lat2, BigDecimal lon2) {
        double p1 = Math.toRadians(lat1.doubleValue());
        double p2 = Math.toRadians(lat2.doubleValue());
        double dp = p2 - p1;
        double dl = Math.toRadians(lon2.subtract(lon1).doubleValue());
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return BigDecimal.valueOf(6371008.8 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private static IdleCandidate.Evidence candidateEvidence(IdleCandidate candidate,
            HistoricalTelemetry fact, Assessment assessment, int credited) {
        return new IdleCandidate.Evidence(UUID.randomUUID(), fact.tenantId(), candidate.candidateId(),
                fact.vehicleId(), fact.deviceId(), fact.eventId(), fact.recordedAt(), fact.dedupeIdentity(),
                EvidenceOutcome.QUALIFYING.name(), fact.engineRunningState().name(),
                fact.engineRunningSource().name(), fact.speedKph(), fact.horizontalAccuracyMeters(),
                assessment.adjustedDistance(), credited);
    }

    private static IdlePersistenceModels.Evidence episodeEvidence(HistoricalTelemetry fact,
            UUID episodeId, Assessment assessment, int credited, EvidenceOutcome outcome) {
        return new IdlePersistenceModels.Evidence(UUID.randomUUID(), fact.tenantId(), episodeId,
                fact.vehicleId(), fact.deviceId(), fact.eventId(), fact.recordedAt(), fact.dedupeIdentity(),
                outcome, fact.engineRunningState() == null ? null : fact.engineRunningState().name(),
                fact.engineRunningSource() == null ? null : fact.engineRunningSource().name(),
                fact.speedKph(), fact.horizontalAccuracyMeters(),
                assessment == null ? null : assessment.adjustedDistance(), credited);
    }

    private void handleCandidateReplayOrConflict(IdleCandidate candidate, HistoricalTelemetry fact,
            Assessment assessment) {
        if (fact.recordedAt().equals(candidate.latestSourceTimestamp())
                && !fact.dedupeIdentity().equals(candidate.lastDedupeIdentity())) {
            IdleCandidate.Evidence conflict = new IdleCandidate.Evidence(UUID.randomUUID(), fact.tenantId(),
                    candidate.candidateId(), fact.vehicleId(), fact.deviceId(), fact.eventId(),
                    fact.recordedAt(), fact.dedupeIdentity(), EvidenceOutcome.CONFLICTING_EVIDENCE.name(),
                    fact.engineRunningState().name(), fact.engineRunningSource().name(), fact.speedKph(),
                    fact.horizontalAccuracyMeters(), assessment.adjustedDistance(), 0);
            candidates.save(candidate, conflict, candidate.version());
            candidates.discard(candidate.tenantId(), candidate.vehicleId(), candidate.candidateId(),
                    candidate.version() + 1);
        }
    }

    private void discardCandidate(HistoricalTelemetry fact) {
        candidates.find(fact.tenantId(), fact.vehicleId()).ifPresent(candidate ->
                candidates.discard(candidate.tenantId(), candidate.vehicleId(), candidate.candidateId(),
                        candidate.version()));
    }

    private record Assessment(boolean qualifying, BigDecimal adjustedDistance) { }
}
