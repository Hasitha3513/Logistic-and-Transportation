package com.transportlogistics.app.tracking.ports.outbound;

import java.time.Instant;
import java.util.UUID;

public interface IdleMonitoringAuditPort {
    void record(UUID tenantId, UUID actorId, String correlationId, String action,
            UUID targetId, String filterShape, int requestedLimit, int resultCount, Instant occurredAt);
}
