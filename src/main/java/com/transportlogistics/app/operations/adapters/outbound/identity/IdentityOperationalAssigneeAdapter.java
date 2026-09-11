package com.transportlogistics.app.operations.adapters.outbound.identity;

import com.transportlogistics.app.identity.OperationalAssignmentDirectory;
import com.transportlogistics.app.operations.ports.outbound.OperationalAssigneeDirectory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class IdentityOperationalAssigneeAdapter implements OperationalAssigneeDirectory {
    private final OperationalAssignmentDirectory identities;

    IdentityOperationalAssigneeAdapter(OperationalAssignmentDirectory identities) {
        this.identities = identities;
    }

    @Override public boolean eligibleUser(UUID tenantId, UUID userId) {
        return identities.eligibleUser(tenantId, userId, "OPERATIONAL_EXCEPTION_MANAGE");
    }
    @Override public boolean activeRole(String roleCode) { return identities.activeRole(roleCode); }
}
