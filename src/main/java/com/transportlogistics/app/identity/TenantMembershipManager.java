package com.transportlogistics.app.identity;

import java.util.UUID;

public interface TenantMembershipManager {
    void ensureActiveMembership(UUID userId, UUID tenantId, String actor);
}
