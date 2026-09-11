package com.transportlogistics.app.operations.ports.outbound;

import java.util.UUID;

public interface OperationalAssigneeDirectory {
    boolean eligibleUser(UUID tenantId, UUID userId);
    boolean activeRole(String roleCode);
}
