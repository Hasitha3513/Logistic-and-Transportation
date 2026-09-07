package com.transportlogistics.app.billing.ports.outbound;

import com.transportlogistics.app.billing.domain.TransportBillingRecord.Source.SourceType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface BillingSourcePort {
    Optional<SourceFact> find(UUID tenantId, SourceType type, UUID sourceId);
    boolean customerActive(UUID tenantId, UUID customerId);
    record SourceFact(SourceType type, UUID id, String businessNumber, String lifecycle,
                      OffsetDateTime completedAt, UUID customerId, long version) {}
}
