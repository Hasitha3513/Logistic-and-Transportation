package com.transportlogistics.app.trip;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** Tenant-scoped immutable Trip projection published for transport billing. */
public interface TripBillingSourceLookup {
    Optional<TripBillingFact> findClosed(UUID tenantId, UUID tripId);

    record TripBillingFact(UUID sourceId, String businessNumber, String lifecycle,
                           OffsetDateTime completedAt, UUID customerId, long sourceVersion) {}
}
