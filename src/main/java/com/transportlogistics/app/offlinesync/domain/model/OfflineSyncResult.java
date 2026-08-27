package com.transportlogistics.app.offlinesync.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfflineSyncResult(
        UUID operationId,
        OfflineSyncResultStatus status,
        OffsetDateTime serverTimestamp,
        UUID aggregateId,
        Long currentVersion,
        String errorCode,
        String message
) {
}
