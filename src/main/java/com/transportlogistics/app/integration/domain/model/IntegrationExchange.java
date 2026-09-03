package com.transportlogistics.app.integration.domain.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record IntegrationExchange(
        UUID id, UUID tenantId, UUID configurationId, UUID sourceEventId, String sourceEventType,
        UUID mappingVersionId, String mappingDefinitionHash, String canonicalPayload, String payloadHash,
        Status status, int attemptCount, OffsetDateTime nextAttemptAt, OffsetDateTime lockedUntil,
        String externalCorrelationId, String targetFilename, String lastErrorCode,
        OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt, long version
) {
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration CLAIM_LEASE = Duration.ofMinutes(5);

    public IntegrationExchange {
        Objects.requireNonNull(id, "Exchange id is required");
        Objects.requireNonNull(tenantId, "Tenant is required");
        Objects.requireNonNull(configurationId, "Configuration is required");
        Objects.requireNonNull(sourceEventId, "Source event is required");
        Objects.requireNonNull(mappingVersionId, "Mapping version is required");
        Objects.requireNonNull(status, "Exchange status is required");
        Objects.requireNonNull(nextAttemptAt, "Next attempt time is required");
        if (attemptCount < 0 || attemptCount > MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Exchange attempt count must be between 0 and 5");
        }
    }

    public static Duration backoffAfterAttempt(int attemptNumber) {
        return switch (attemptNumber) {
            case 1 -> Duration.ofSeconds(30);
            case 2 -> Duration.ofMinutes(2);
            case 3 -> Duration.ofMinutes(10);
            case 4 -> Duration.ofMinutes(30);
            default -> Duration.ZERO;
        };
    }

    public enum Status { PENDING, IN_PROGRESS, RETRY_SCHEDULED, SUCCEEDED, FAILED }
}
