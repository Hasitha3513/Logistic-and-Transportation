package com.transportlogistics.app.tracking.ports.outbound;

import java.time.Instant;
import java.util.UUID;

public interface GeofenceManagementSupportPort {
    Claim claim(UUID tenantId, String scope, String key, String requestHash,
                UUID targetId, UUID actorId, Instant now);

    void complete(UUID claimId, long resultVersion);

    void audit(UUID tenantId, UUID actorId, String action, UUID geofenceId,
               String safeDetail, Instant now);

    record Claim(UUID claimId, UUID targetId, String requestHash,
                 Long resultVersion, boolean acquired) {
    }
}
