package com.transportlogistics.app.offlinesync.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record OfflineSyncOperation(
        UUID operationId,
        String operationType,
        int operationVersion,
        UUID actorId,
        UUID clientInstanceId,
        String aggregateType,
        UUID aggregateId,
        String requestHash,
        OfflineSyncResultStatus resultStatus,
        String resultCode,
        Long resultVersion,
        OffsetDateTime processedAt,
        OffsetDateTime createdAt
) {
    public OfflineSyncOperation {
        Objects.requireNonNull(operationId, "Operation ID is required");
        operationType = required(operationType, "Operation type", 64);
        if (operationVersion <= 0) {
            throw new IllegalArgumentException("Operation version must be positive");
        }
        Objects.requireNonNull(actorId, "Actor ID is required");
        Objects.requireNonNull(clientInstanceId, "Client instance ID is required");
        aggregateType = required(aggregateType, "Aggregate type", 32);
        Objects.requireNonNull(aggregateId, "Aggregate ID is required");
        requestHash = required(requestHash, "Request hash", 64);
        Objects.requireNonNull(resultStatus, "Result status is required");
        if (!resultStatus.stored()) {
            throw new IllegalArgumentException("Only terminal inbox results may be stored");
        }
        resultCode = optional(resultCode, 64);
        Objects.requireNonNull(processedAt, "Processed time is required");
        Objects.requireNonNull(createdAt, "Created time is required");
    }

    public OfflineSyncOperation withResult(OfflineSyncResultStatus status, String code, OffsetDateTime time) {
        return new OfflineSyncOperation(operationId, operationType, operationVersion, actorId, clientInstanceId,
                aggregateType, aggregateId, requestHash, status, code, null, time, createdAt);
    }

    private static String required(String value, String label, int maximum) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maximum) {
            throw new IllegalArgumentException(label + " is too long");
        }
        return normalized;
    }

    private static String optional(String value, int maximum) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.substring(0, Math.min(maximum, normalized.length()));
    }
}
