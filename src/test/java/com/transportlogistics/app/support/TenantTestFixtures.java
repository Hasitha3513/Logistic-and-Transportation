package com.transportlogistics.app.support;

import com.transportlogistics.app.tenancy.CanonicalTenant;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class TenantTestFixtures {
    private TenantTestFixtures() {
    }

    public static void canonicalMembership(JdbcTemplate jdbc, UUID userId) {
        var membershipId = UUID.nameUUIDFromBytes(("test-membership:" + userId).getBytes(StandardCharsets.UTF_8));
        var now = OffsetDateTime.now();
        jdbc.update("""
                INSERT INTO tenant_membership (membership_id, tenant_id, user_id, status, created_at, created_by,
                    updated_at, updated_by, version) VALUES (?, ?, ?, 'ACTIVE', ?, 'test', ?, 'test', 0)
                """, membershipId, CanonicalTenant.ID, userId, now, now);
    }

    public static void assignCanonicalRole(JdbcTemplate jdbc, UUID userId, UUID roleId) {
        jdbc.update("""
                INSERT INTO tenant_membership_role (membership_id, role_id)
                SELECT membership_id, ? FROM tenant_membership
                WHERE user_id = ? AND tenant_id = ? AND status = 'ACTIVE'
                """, roleId, userId, CanonicalTenant.ID);
    }
}
