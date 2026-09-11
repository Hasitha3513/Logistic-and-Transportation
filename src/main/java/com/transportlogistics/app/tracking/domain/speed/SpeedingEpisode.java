package com.transportlogistics.app.tracking.domain.speed;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpeedingEpisode(UUID id, UUID tenantId, UUID vehicleId, SpeedAttribution attribution,
                              UUID ruleId, long ruleVersion,
                              ResolvedSpeedThreshold.ThresholdSource thresholdSource,
                              SpeedKph effectiveThresholdKph, Instant startSourceTimestamp,
                              Instant confirmationSourceTimestamp, Instant endSourceTimestamp,
                              UUID firstPositionId, UUID confirmingPositionId,
                              SpeedKph maxObservedSpeedKph, int eligibleAboveThresholdSampleCount,
                              Severity severity, int repeatCount) {
    private static final Duration REPEAT_WINDOW = Duration.ofMinutes(10);

    public enum Severity { WARNING, HIGH }

    public SpeedingEpisode {
        Objects.requireNonNull(id, "Episode ID is required");
        Objects.requireNonNull(tenantId, "Tenant ID is required");
        Objects.requireNonNull(vehicleId, "Vehicle ID is required");
        attribution = attribution == null ? SpeedAttribution.unknown() : attribution;
        Objects.requireNonNull(ruleId, "Rule ID is required");
        if (ruleVersion < 1) throw new IllegalArgumentException("Rule version must be positive");
        Objects.requireNonNull(thresholdSource, "Threshold source is required");
        Objects.requireNonNull(effectiveThresholdKph, "Threshold is required");
        Objects.requireNonNull(startSourceTimestamp, "Start source time is required");
        Objects.requireNonNull(confirmationSourceTimestamp, "Confirmation source time is required");
        Objects.requireNonNull(firstPositionId, "First position ID is required");
        Objects.requireNonNull(confirmingPositionId, "Confirming position ID is required");
        Objects.requireNonNull(maxObservedSpeedKph, "Maximum observed speed is required");
        Objects.requireNonNull(severity, "Severity is required");
        if (confirmationSourceTimestamp.isBefore(startSourceTimestamp)
                || endSourceTimestamp != null && endSourceTimestamp.isBefore(confirmationSourceTimestamp)
                || eligibleAboveThresholdSampleCount < 2 || repeatCount < 0) {
            throw new IllegalArgumentException("Episode chronology or counts are invalid");
        }
    }

    public static SpeedingEpisode confirm(UUID tenantId, UUID vehicleId,
                                           ResolvedSpeedThreshold threshold,
                                           SpeedAttribution attribution,
                                           UUID firstPositionId, Instant firstSourceTime,
                                           SpeedKph firstSpeed, SpeedPosition confirming,
                                           SpeedingEpisode previousClosed) {
        boolean repeat = isRepeat(previousClosed, tenantId, vehicleId, threshold.rule(), firstSourceTime);
        int repeats = repeat ? previousClosed.repeatCount + 1 : 0;
        SpeedKph maximum = firstSpeed.compareTo(confirming.speedKph()) >= 0
                ? firstSpeed : confirming.speedKph();
        UUID episodeId = SpeedingEpisodeIdentity.create(tenantId, vehicleId, threshold.rule().id(),
                threshold.rule().ruleVersion(), firstPositionId);
        return new SpeedingEpisode(episodeId, tenantId, vehicleId, attribution,
                threshold.rule().id(), threshold.rule().ruleVersion(), threshold.source(),
                threshold.rule().thresholdKph(), firstSourceTime, confirming.sourceTimestamp(), null,
                firstPositionId, confirming.positionId(), maximum, 2,
                repeat ? Severity.HIGH : Severity.WARNING, repeats);
    }

    public SpeedingEpisode progress(SpeedPosition position) {
        requireOpenScope(position);
        SpeedKph maximum = maxObservedSpeedKph.compareTo(position.speedKph()) >= 0
                ? maxObservedSpeedKph : position.speedKph();
        return new SpeedingEpisode(id, tenantId, vehicleId, attribution, ruleId, ruleVersion,
                thresholdSource, effectiveThresholdKph, startSourceTimestamp,
                confirmationSourceTimestamp, null, firstPositionId, confirmingPositionId,
                maximum, eligibleAboveThresholdSampleCount + 1, severity, repeatCount);
    }

    public SpeedingEpisode close(SpeedPosition position) {
        requireOpenScope(position);
        return new SpeedingEpisode(id, tenantId, vehicleId, attribution, ruleId, ruleVersion,
                thresholdSource, effectiveThresholdKph, startSourceTimestamp,
                confirmationSourceTimestamp, position.sourceTimestamp(), firstPositionId,
                confirmingPositionId, maxObservedSpeedKph, eligibleAboveThresholdSampleCount,
                severity, repeatCount);
    }

    public boolean open() {
        return endSourceTimestamp == null;
    }

    private void requireOpenScope(SpeedPosition position) {
        if (!open() || !tenantId.equals(position.tenantId()) || !vehicleId.equals(position.vehicleId())) {
            throw new SpeedMonitoringException("SPEED_EPISODE_SCOPE_INVALID", "Open episode scope is required");
        }
    }

    private static boolean isRepeat(SpeedingEpisode previous, UUID tenantId, UUID vehicleId,
                                    SpeedRule rule, Instant startTime) {
        if (previous == null || previous.open() || !tenantId.equals(previous.tenantId)
                || !vehicleId.equals(previous.vehicleId) || !rule.id().equals(previous.ruleId)
                || rule.ruleVersion() != previous.ruleVersion || startTime.isBefore(previous.endSourceTimestamp)) {
            return false;
        }
        return Duration.between(previous.endSourceTimestamp, startTime).compareTo(REPEAT_WINDOW) <= 0;
    }
}
