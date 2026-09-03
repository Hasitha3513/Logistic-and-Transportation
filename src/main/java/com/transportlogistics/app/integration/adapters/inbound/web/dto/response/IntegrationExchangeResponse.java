package com.transportlogistics.app.integration.adapters.inbound.web.dto.response;

import com.transportlogistics.app.integration.domain.model.IntegrationExchange;
import com.transportlogistics.app.integration.domain.model.IntegrationExchangeAttempt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IntegrationExchangeResponse(
        UUID id, UUID sourceEventId, String sourceEventType, UUID mappingVersionId, String mappingDefinitionHash,
        String payloadHash, IntegrationExchange.Status status, int attemptCount, OffsetDateTime nextAttemptAt,
        String externalCorrelationId, String targetFilename, String lastErrorCode, OffsetDateTime createdAt,
        OffsetDateTime updatedAt, OffsetDateTime completedAt, List<AttemptResponse> attempts
) {
    public record AttemptResponse(int attemptNumber, OffsetDateTime startedAt, OffsetDateTime completedAt,
                                  long latencyMillis, IntegrationExchangeAttempt.Outcome outcome, String errorCode,
                                  String externalCorrelationId, String targetFilename) {}
}
