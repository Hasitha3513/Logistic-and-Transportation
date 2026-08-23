package com.transportlogistics.app.offlinesync.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OfflineOperationContext(
        UUID operationId,
        UUID actorId,
        String actorName,
        UUID aggregateId,
        OffsetDateTime clientCreatedAt
) {
}
