package com.transportlogistics.app.identity.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantMembership(UUID membershipId, UUID tenantId, UUID userId, TenantMembershipStatus status,
                               OffsetDateTime createdAt, String createdBy, OffsetDateTime updatedAt,
                               String updatedBy, long version) {
    public TenantMembership {
        if (membershipId == null || tenantId == null || userId == null || status == null || createdAt == null
                || createdBy == null || createdBy.isBlank() || updatedAt == null || updatedBy == null
                || updatedBy.isBlank() || version < 0) {
            throw new IllegalArgumentException("Tenant membership fields are invalid");
        }
    }
}
