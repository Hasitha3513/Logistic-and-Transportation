package com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfflineSyncOperationRequest(
        @NotNull(message = "operationId is required") UUID operationId,
        @NotBlank(message = "operationType is required") String operationType,
        @Positive(message = "operationVersion must be positive") int operationVersion,
        @NotBlank(message = "aggregateType is required") String aggregateType,
        @NotNull(message = "aggregateId is required") UUID aggregateId,
        @NotNull(message = "payload is required") JsonNode payload,
        @NotNull(message = "clientCreatedAt is required") OffsetDateTime clientCreatedAt,
        @NotNull(message = "clientUpdatedAt is required") OffsetDateTime clientUpdatedAt,
        @NotNull(message = "clientInstanceId is required") UUID clientInstanceId,
        @NotBlank(message = "idempotencyKey is required") String idempotencyKey,
        @Null(message = "baseVersion must be null for version 1") Long baseVersion
) {
}
