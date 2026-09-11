package com.transportlogistics.app.freight;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Freight-owned publication boundary for explicit terminal billable facts. */
public interface FreightBillingFactPublisher {
    void publishTerminal(UUID tenantId, UUID freightOrderId, String lifecycle,
                         OffsetDateTime completedAt, long sourceVersion);
}
