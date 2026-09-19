package com.transportlogistics.app.identity.domain.risk;

import static com.transportlogistics.app.identity.domain.risk.UserRiskFirstWaveContract.EvidenceState;
import static com.transportlogistics.app.identity.domain.risk.UserRiskFirstWaveContract.SignalComparison;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Minimized provider-neutral fact for an Identity permission-ceiling denial. */
public record PermissionCeilingDenialSignal(
        UUID eventId,
        UUID tenantId,
        UserRiskFirstWaveContract.SourceModule sourceModule,
        UUID sourceEventId,
        UUID subjectUserId,
        UUID actorUserId,
        UserRiskFirstWaveContract.Action action,
        UserRiskFirstWaveContract.Reason reason,
        UserRiskFirstWaveContract.TargetType targetType,
        UUID targetId,
        Instant occurredAt,
        Instant receivedAt,
        String correlationId,
        EvidenceState evidenceState) {

    private static final int MAX_CORRELATION_ID_LENGTH = 128;

    public PermissionCeilingDenialSignal {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(sourceModule, "sourceModule must not be null");
        Objects.requireNonNull(sourceEventId, "sourceEventId must not be null");
        Objects.requireNonNull(subjectUserId, "subjectUserId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        Objects.requireNonNull(evidenceState, "evidenceState must not be null");
        if (sourceModule != UserRiskFirstWaveContract.SourceModule.IDENTITY
                || !sourceEventId.equals(eventId)) {
            throw new IllegalArgumentException("The first-wave source must be Identity and use eventId as sourceEventId");
        }
        if (!subjectUserId.equals(actorUserId)) {
            throw new IllegalArgumentException("The first-wave signal subject must be the actor");
        }
        if (targetType != expectedTargetType(action)) {
            throw new IllegalArgumentException("targetType must match the approved action");
        }
        correlationId = requiredCode(correlationId, "correlationId", MAX_CORRELATION_ID_LENGTH);
        if (!eventId.equals(retryIdentity(tenantId, actorUserId, action, targetType, targetId, correlationId))) {
            throw new IllegalArgumentException("eventId must match the deterministic retry identity");
        }
        if (receivedAt.isBefore(occurredAt)) {
            throw new IllegalArgumentException("receivedAt must not precede occurredAt");
        }
    }

    public static UUID retryIdentity(
            UUID tenantId,
            UUID actorUserId,
            UserRiskFirstWaveContract.Action action,
            UserRiskFirstWaveContract.TargetType targetType,
            UUID targetId,
            String correlationId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        String correlation = requiredCode(correlationId, "correlationId", MAX_CORRELATION_ID_LENGTH);
        String canonical = tenantId + "|" + actorUserId + "|" + action + "|" + targetType + "|"
                + (targetId == null ? "NONE" : targetId) + "|" + correlation;
        return sha256Uuid(canonical);
    }

    public UserRiskFirstWaveContract.SignalType signalType() {
        return UserRiskFirstWaveContract.SignalType.UNAUTHORIZED_OVERRIDE_ATTEMPT;
    }

    public UserRiskFirstWaveContract.ActionFamily actionFamily() {
        return action.family();
    }

    public boolean mayContributeToFinding() {
        return evidenceState.mayContributeToFinding();
    }

    public SignalComparison compareIdentity(PermissionCeilingDenialSignal other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!tenantId.equals(other.tenantId) || !eventId.equals(other.eventId)) {
            return SignalComparison.DISTINCT;
        }
        return equals(other) ? SignalComparison.DUPLICATE : SignalComparison.CONFLICTING;
    }

    private static String requiredCode(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength || !normalized.matches("[A-Za-z0-9][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException(field + " must be an opaque safe code");
        }
        return normalized;
    }

    private static UserRiskFirstWaveContract.TargetType expectedTargetType(
            UserRiskFirstWaveContract.Action action) {
        return switch (action) {
            case IDENTITY_USER_CREATE_ROLE_PERMISSION_CEILING,
                    IDENTITY_USER_UPDATE_ROLE_PERMISSION_CEILING -> UserRiskFirstWaveContract.TargetType.USER;
            case IDENTITY_ROLE_CREATE_PERMISSION_CEILING,
                    IDENTITY_ROLE_UPDATE_PERMISSION_CEILING -> UserRiskFirstWaveContract.TargetType.ROLE;
        };
    }

    private static UUID sha256Uuid(String canonical) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
            ByteBuffer bytes = ByteBuffer.wrap(hash);
            return new UUID(bytes.getLong(), bytes.getLong());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
