package com.transportlogistics.app.offlinesync.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.offlinesync.domain.model.OfflineSyncResultStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfflineSyncOperationResponse(
        UUID operationId,
        OfflineSyncResultStatus status,
        OffsetDateTime serverTimestamp,
        UUID aggregateId,
        Long currentVersion,
        String errorCode,
        String message
) {
}
