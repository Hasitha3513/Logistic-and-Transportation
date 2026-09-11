package com.transportlogistics.app.integration.ports.outbound;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface IntegrationRateLimiter {
    boolean allow(UUID tenantId, UUID configurationId, String actor, OffsetDateTime now);
}
