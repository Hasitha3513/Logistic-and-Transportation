package com.transportlogistics.app.billing;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface BillingDeliveryPort {
    void fileDelivered(UUID tenantId, UUID sourceEventId, String payloadHash,
                       String targetFilename, OffsetDateTime deliveredAt);
}
