package com.transportlogistics.app.identity;

import java.util.UUID;

/** Public Identity contract used to validate Operations assignment targets. */
public interface OperationalAssignmentDirectory {
    boolean eligibleUser(UUID tenantId, UUID userId, String requiredPermission);
    boolean activeRole(String roleCode);
}
