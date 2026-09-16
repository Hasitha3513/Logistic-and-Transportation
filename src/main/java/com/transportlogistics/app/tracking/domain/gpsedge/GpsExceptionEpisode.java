package com.transportlogistics.app.tracking.domain.gpsedge;

import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.EpisodeStatus;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.ExceptionType;
import com.transportlogistics.app.tracking.domain.gpsedge.GpsReliabilityModels.Severity;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GpsExceptionEpisode(
        UUID id,
        UUID tenantId,
        UUID deviceId,
        UUID vehicleId,
        ExceptionType type,
        Severity severity,
        EpisodeStatus status,
        Instant openedAt,
        Instant lastObservedAt,
        Instant resolvedAt,
        long evidenceCount,
        int consecutiveRecoveryPoints,
        long version,
        String acknowledgementReason) {

    public GpsExceptionEpisode {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(status);
        Objects.requireNonNull(openedAt);
        Objects.requireNonNull(lastObservedAt);
        if (evidenceCount < 1 || consecutiveRecoveryPoints < 0 || version < 0) {
            throw new GpsEdgeCaseException("INVALID_EPISODE", "Episode counters and version must be valid");
        }
        if (status == EpisodeStatus.RESOLVED && resolvedAt == null) {
            throw new GpsEdgeCaseException("INVALID_EPISODE", "Resolved episode requires resolvedAt");
        }
        if (status != EpisodeStatus.RESOLVED && resolvedAt != null) {
            throw new GpsEdgeCaseException("INVALID_EPISODE", "Open episode cannot have resolvedAt");
        }
    }

    public static GpsExceptionEpisode open(
            UUID id,
            UUID tenantId,
            UUID deviceId,
            UUID vehicleId,
            ExceptionType type,
            Severity severity,
            Instant observedAt) {
        return new GpsExceptionEpisode(id, tenantId, deviceId, vehicleId, type, severity,
                EpisodeStatus.OPEN, observedAt, observedAt, null, 1, 0, 0, null);
    }

    public GpsExceptionEpisode observe(Severity observedSeverity, Instant observedAt) {
        requireActive();
        if (observedAt.isBefore(lastObservedAt)) {
            throw new GpsEdgeCaseException("STALE_EPISODE_EVIDENCE", "Evidence cannot regress episode time");
        }
        Severity nextSeverity = severity == Severity.HIGH ? Severity.HIGH : observedSeverity;
        return copy(nextSeverity, status == EpisodeStatus.RECOVERING ? EpisodeStatus.OPEN : status,
                observedAt, null, evidenceCount + 1, 0, version + 1, acknowledgementReason);
    }

    public GpsExceptionEpisode acknowledge(String reason, Instant acknowledgedAt) {
        requireActive();
        if (reason == null || reason.isBlank()) {
            throw new GpsEdgeCaseException("ACKNOWLEDGEMENT_REASON_REQUIRED", "A review reason is required");
        }
        if (acknowledgedAt.isBefore(lastObservedAt)) {
            throw new GpsEdgeCaseException("STALE_EPISODE_ACTION", "Acknowledgement time is stale");
        }
        return copy(severity, EpisodeStatus.ACKNOWLEDGED, acknowledgedAt, null,
                evidenceCount, 0, version + 1, reason.trim());
    }

    public GpsExceptionEpisode recover(Instant observedAt) {
        requireActive();
        if (!systemRecoverable()) {
            throw new GpsEdgeCaseException("CORRECTION_REQUIRED", "This exception requires confirmed correction");
        }
        if (observedAt.isBefore(lastObservedAt)) {
            throw new GpsEdgeCaseException("STALE_EPISODE_ACTION", "Recovery time is stale");
        }
        int nextRecoveryCount = consecutiveRecoveryPoints + 1;
        if (nextRecoveryCount < GpsReliabilityPolicy.REQUIRED_RECOVERY_POINTS) {
            return copy(severity, EpisodeStatus.RECOVERING, observedAt, null,
                    evidenceCount, nextRecoveryCount, version + 1, acknowledgementReason);
        }
        return copy(severity, EpisodeStatus.RESOLVED, observedAt, observedAt,
                evidenceCount, nextRecoveryCount, version + 1, acknowledgementReason);
    }

    public GpsExceptionEpisode resolveAfterCorrection(Instant correctedAt) {
        requireActive();
        if (systemRecoverable()) {
            throw new GpsEdgeCaseException("AUTOMATIC_RECOVERY_REQUIRED", "Observable exception requires recovery evidence");
        }
        if (correctedAt.isBefore(lastObservedAt)) {
            throw new GpsEdgeCaseException("STALE_EPISODE_ACTION", "Correction time is stale");
        }
        return copy(severity, EpisodeStatus.RESOLVED, correctedAt, correctedAt,
                evidenceCount, 0, version + 1, acknowledgementReason);
    }

    public boolean systemRecoverable() {
        return switch (type) {
            case IMPOSSIBLE_MOVEMENT, SIGNAL_LOSS, DEVICE_TAMPER, BATTERY_LOW, BATTERY_RAPID_DRAIN,
                    CLOCK_ANOMALY, LOW_ACCURACY, INVALID_TELEMETRY -> true;
            case BINDING_VIOLATION, PROCESSING_FAILURE -> false;
        };
    }

    private void requireActive() {
        if (status == EpisodeStatus.RESOLVED) {
            throw new GpsEdgeCaseException("EPISODE_RESOLVED", "Resolved episode is immutable");
        }
    }

    private GpsExceptionEpisode copy(
            Severity nextSeverity,
            EpisodeStatus nextStatus,
            Instant nextObservedAt,
            Instant nextResolvedAt,
            long nextEvidenceCount,
            int nextRecoveryCount,
            long nextVersion,
            String nextReason) {
        return new GpsExceptionEpisode(id, tenantId, deviceId, vehicleId, type, nextSeverity, nextStatus,
                openedAt, nextObservedAt, nextResolvedAt, nextEvidenceCount, nextRecoveryCount,
                nextVersion, nextReason);
    }
}
