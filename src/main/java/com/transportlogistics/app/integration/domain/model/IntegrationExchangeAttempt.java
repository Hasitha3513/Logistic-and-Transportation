package com.transportlogistics.app.integration.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntegrationExchangeAttempt(
        UUID id, UUID tenantId, UUID exchangeId, int attemptNumber, OffsetDateTime startedAt,
        OffsetDateTime completedAt, long latencyMillis, Outcome outcome, String errorCode,
        String externalCorrelationId, String targetFilename
) {
    public enum Outcome { SUCCEEDED, RETRYABLE_FAILURE, PERMANENT_FAILURE }
}
