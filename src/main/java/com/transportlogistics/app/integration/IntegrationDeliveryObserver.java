package com.transportlogistics.app.integration;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Published provider-neutral observation of a durably recorded outbound delivery. */
public interface IntegrationDeliveryObserver {
    void delivered(UUID tenantId, UUID sourceEventId, String sourceEventType,
                   String payloadHash, String targetFilename, OffsetDateTime deliveredAt);
}
