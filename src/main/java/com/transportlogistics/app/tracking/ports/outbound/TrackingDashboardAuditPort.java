package com.transportlogistics.app.tracking.ports.outbound;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface TrackingDashboardAuditPort {
    void record(UUID tenantId, UUID actorId, String correlationId, String action,
                Set<String> filterCategories, int requestedPageSize, int resultCount,
                Set<String> includedSections, Set<String> sourceStatuses, Instant occurredAt);
}
