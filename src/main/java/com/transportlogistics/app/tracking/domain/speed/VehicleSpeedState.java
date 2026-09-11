package com.transportlogistics.app.tracking.domain.speed;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record VehicleSpeedState(UUID tenantId, UUID vehicleId, MonitoringState monitoringState,
                                Availability availability, UUID effectiveRuleId,
                                long effectiveRuleVersion, UUID candidateFirstPositionId,
                                Instant candidateSourceTimestamp, SpeedKph candidateObservedSpeed,
                                int candidateSampleCount, UUID activeEpisodeId,
                                Instant lastEvaluatedSourceTimestamp, UUID lastEvaluatedPositionId,
                                long version) {
    public enum MonitoringState { UNKNOWN, NORMAL, SPEEDING }
    public enum Availability { AVAILABLE, NOT_EVALUATED, CONFIGURATION_UNAVAILABLE }

    public VehicleSpeedState {
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(vehicleId, "Vehicle ID is required");
        Objects.requireNonNull(monitoringState, "Monitoring state is required");
        Objects.requireNonNull(availability, "Availability is required");
        if (effectiveRuleVersion < 0 || candidateSampleCount < 0 || candidateSampleCount > 1 || version < 0) {
            throw new IllegalArgumentException("Speed state versions or candidate count are invalid");
        }
        boolean candidateAbsent = candidateFirstPositionId == null;
        if (candidateAbsent != (candidateSourceTimestamp == null)
                || candidateAbsent != (candidateObservedSpeed == null)
                || candidateAbsent != (candidateSampleCount == 0)) {
            throw new IllegalArgumentException("Candidate facts must be complete");
        }
        if ((monitoringState == MonitoringState.SPEEDING) != (activeEpisodeId != null)) {
            throw new IllegalArgumentException("Speeding state requires exactly one active episode");
        }
    }

    public static VehicleSpeedState unknown(UUID tenantId, UUID vehicleId) {
        return new VehicleSpeedState(tenantId, vehicleId, MonitoringState.UNKNOWN,
                Availability.NOT_EVALUATED, null, 0, null, null, null, 0,
                null, null, null, 0);
    }

    public SpeedEvaluationResult evaluate(SpeedPosition position,
                                          Optional<ResolvedSpeedThreshold> resolvedThreshold,
                                          SpeedAttribution attribution, Instant evaluatedAt,
                                          SpeedingEpisode activeEpisode,
                                          SpeedingEpisode previousClosedEpisode) {
        requireScope(position);
        if (position.eligibilityAt(evaluatedAt) != SpeedPosition.Eligibility.ELIGIBLE
                || !isAfterLast(position)) {
            return SpeedEvaluationResult.of(SpeedEvaluationResult.Outcome.NO_OP, this);
        }
        if (resolvedThreshold.isEmpty()) {
            return SpeedEvaluationResult.of(SpeedEvaluationResult.Outcome.CONFIGURATION_UNAVAILABLE,
                    reset(position, Availability.CONFIGURATION_UNAVAILABLE));
        }
        ResolvedSpeedThreshold threshold = resolvedThreshold.get();
        if (!tenantId.equals(threshold.rule().tenantId())) {
            throw new SpeedMonitoringException("SPEED_RULE_SCOPE_INVALID", "Rule Tenant does not match state");
        }
        if (ruleChanged(threshold)) {
            return resetForRule(threshold).evaluate(position, resolvedThreshold, attribution,
                    evaluatedAt, null, previousClosedEpisode);
        }
        boolean above = position.speedKph().exceeds(threshold.rule().thresholdKph());
        if (monitoringState == MonitoringState.SPEEDING) {
            if (activeEpisode == null || !activeEpisode.id().equals(activeEpisodeId)) {
                throw new SpeedMonitoringException("SPEED_EPISODE_REQUIRED", "Active episode is required");
            }
            if (above) {
                SpeedingEpisode progressed = activeEpisode.progress(position);
                return SpeedEvaluationResult.withEpisode(SpeedEvaluationResult.Outcome.EPISODE_PROGRESSED,
                        next(MonitoringState.SPEEDING, Availability.AVAILABLE, threshold,
                                null, null, null, 0, activeEpisodeId, position), progressed);
            }
            SpeedingEpisode closed = activeEpisode.close(position);
            return SpeedEvaluationResult.withEpisode(SpeedEvaluationResult.Outcome.EPISODE_CLOSED,
                    next(MonitoringState.NORMAL, Availability.AVAILABLE, threshold,
                            null, null, null, 0, null, position), closed);
        }
        if (!above) {
            return SpeedEvaluationResult.of(SpeedEvaluationResult.Outcome.STATE_INITIALIZED,
                    next(MonitoringState.NORMAL, Availability.AVAILABLE, threshold,
                            null, null, null, 0, null, position));
        }
        if (candidateFirstPositionId == null) {
            return SpeedEvaluationResult.of(SpeedEvaluationResult.Outcome.CANDIDATE_UPDATED,
                    next(monitoringState == MonitoringState.UNKNOWN ? MonitoringState.UNKNOWN : MonitoringState.NORMAL,
                            Availability.AVAILABLE, threshold, position.positionId(),
                            position.sourceTimestamp(), position.speedKph(), 1, null, position));
        }
        if (candidateFirstPositionId.equals(position.positionId())) {
            return SpeedEvaluationResult.of(SpeedEvaluationResult.Outcome.NO_OP, this);
        }
        SpeedingEpisode episode = SpeedingEpisode.confirm(tenantId, vehicleId, threshold,
                attribution, candidateFirstPositionId, candidateSourceTimestamp,
                candidateObservedSpeed, position, previousClosedEpisode);
        return SpeedEvaluationResult.withEpisode(SpeedEvaluationResult.Outcome.EPISODE_CONFIRMED,
                next(MonitoringState.SPEEDING, Availability.AVAILABLE, threshold,
                        null, null, null, 0, episode.id(), position), episode);
    }

    private VehicleSpeedState resetForRule(ResolvedSpeedThreshold threshold) {
        return next(MonitoringState.UNKNOWN, Availability.AVAILABLE, threshold,
                null, null, null, 0, null, null);
    }

    private VehicleSpeedState reset(SpeedPosition position, Availability reason) {
        return new VehicleSpeedState(tenantId, vehicleId, MonitoringState.UNKNOWN, reason,
                null, 0, null, null, null, 0, null, position.sourceTimestamp(),
                position.positionId(), version + 1);
    }

    private boolean ruleChanged(ResolvedSpeedThreshold threshold) {
        return effectiveRuleId == null || !effectiveRuleId.equals(threshold.rule().id())
                || effectiveRuleVersion != threshold.rule().ruleVersion();
    }

    private boolean isAfterLast(SpeedPosition position) {
        if (lastEvaluatedSourceTimestamp == null) return true;
        int comparison = position.sourceTimestamp().compareTo(lastEvaluatedSourceTimestamp);
        return comparison > 0 || comparison == 0
                && (lastEvaluatedPositionId == null
                || position.positionId().compareTo(lastEvaluatedPositionId) > 0);
    }

    private VehicleSpeedState next(MonitoringState state, Availability nextAvailability,
                                   ResolvedSpeedThreshold threshold, UUID candidateId,
                                   Instant candidateTime, SpeedKph candidateSpeed, int candidateCount,
                                   UUID episodeId, SpeedPosition position) {
        return new VehicleSpeedState(tenantId, vehicleId, state, nextAvailability,
                threshold.rule().id(), threshold.rule().ruleVersion(), candidateId,
                candidateTime, candidateSpeed, candidateCount, episodeId,
                position == null ? lastEvaluatedSourceTimestamp : position.sourceTimestamp(),
                position == null ? lastEvaluatedPositionId : position.positionId(), version + 1);
    }

    private void requireScope(SpeedPosition position) {
        if (!tenantId.equals(position.tenantId()) || !vehicleId.equals(position.vehicleId())) {
            throw new SpeedMonitoringException("SPEED_EVALUATION_SCOPE_INVALID",
                    "Position and speed state must share Tenant and Vehicle");
        }
    }
}
