package com.transportlogistics.app.tracking.domain.gpsedge;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GpsReliabilityModels {
    private GpsReliabilityModels() {
    }

    public enum Trust {
        TRUSTED, UNTRUSTED, UNKNOWN
    }

    public enum Quality {
        NORMAL,
        ACCURACY_UNKNOWN,
        LOW_ACCURACY,
        UNUSABLE_ACCURACY,
        NULL_ISLAND_SUSPECT,
        CLOCK_SKEW,
        DELAYED,
        LATE,
        OUT_OF_ORDER,
        FUTURE,
        IMPOSSIBLE_MOVEMENT,
        TAMPER_ACTIVE,
        BATTERY_LOW,
        BATTERY_CRITICAL,
        BATTERY_RAPID_DRAIN,
        BINDING_INVALID
    }

    public enum Ordering {
        IN_ORDER, OUT_OF_ORDER, EQUAL_SOURCE_TIME
    }

    public enum ReliabilityState {
        NORMAL, DEGRADED, SUSPECT, OFFLINE, RECOVERING, UNKNOWN
    }

    public enum Connectivity {
        LIVE, RECENT, STALE, OFFLINE, NEVER_SEEN
    }

    public enum SignalState {
        ACTIVE, CLEAR, UNKNOWN
    }

    public enum ExceptionType {
        INVALID_TELEMETRY,
        CLOCK_ANOMALY,
        LOW_ACCURACY,
        IMPOSSIBLE_MOVEMENT,
        SIGNAL_LOSS,
        DEVICE_TAMPER,
        BATTERY_LOW,
        BATTERY_RAPID_DRAIN,
        BINDING_VIOLATION,
        PROCESSING_FAILURE
    }

    public enum Severity {
        WARNING, HIGH
    }

    public enum EpisodeStatus {
        OPEN, ACKNOWLEDGED, RECOVERING, RESOLVED
    }

    public record Observation(
            UUID tenantId,
            UUID deviceId,
            UUID vehicleId,
            UUID eventId,
            GpsCoordinate coordinate,
            Double accuracyMeters,
            Instant sourceTimestamp,
            Instant receivedAt,
            SignalState tamperState,
            BigDecimal batteryPercentage) {
        public Observation {
            Objects.requireNonNull(tenantId, "tenantId is required");
            Objects.requireNonNull(deviceId, "deviceId is required");
            Objects.requireNonNull(eventId, "eventId is required");
            Objects.requireNonNull(coordinate, "coordinate is required");
            Objects.requireNonNull(sourceTimestamp, "sourceTimestamp is required");
            Objects.requireNonNull(receivedAt, "receivedAt is required");
            tamperState = tamperState == null ? SignalState.UNKNOWN : tamperState;
            if (accuracyMeters != null && (!Double.isFinite(accuracyMeters)
                    || accuracyMeters <= 0 || accuracyMeters > 10_000)) {
                throw new GpsEdgeCaseException("INVALID_ACCURACY", "Accuracy must be greater than zero and at most 10000 metres");
            }
            if (batteryPercentage != null && (batteryPercentage.compareTo(BigDecimal.ZERO) < 0
                    || batteryPercentage.compareTo(BigDecimal.valueOf(100)) > 0
                    || Math.max(batteryPercentage.scale(), 0) > 3)) {
                throw new GpsEdgeCaseException("INVALID_BATTERY", "Battery percentage must be between zero and 100");
            }
        }
    }

    public record EvaluationContext(
            Instant evaluatedAt,
            Instant retainedAfter,
            Observation latestTrusted,
            boolean bindingValid,
            boolean recoveryHold) {
        public EvaluationContext {
            Objects.requireNonNull(evaluatedAt, "evaluatedAt is required");
            Objects.requireNonNull(retainedAfter, "retainedAfter is required");
        }
    }

    public record Assessment(
            Trust trust,
            Ordering ordering,
            Connectivity connectivity,
            ReliabilityState state,
            Set<Quality> qualities,
            boolean latestTrustedEligible,
            boolean detectorEligible) {
        public Assessment {
            Objects.requireNonNull(trust);
            Objects.requireNonNull(ordering);
            Objects.requireNonNull(connectivity);
            Objects.requireNonNull(state);
            qualities = Set.copyOf(qualities);
        }
    }
}
