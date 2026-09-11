package com.transportlogistics.app.freight;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Freight-owned terminal fact; Billing must never infer completion from order fields. */
public interface FreightBillingSourceLookup {
    Optional<FreightBillingFact> findTerminal(UUID tenantId, UUID freightOrderId);

    record FreightBillingFact(UUID sourceId, String businessNumber, String lifecycle,
                              OffsetDateTime completedAt, UUID customerId, long sourceVersion) {}
}
