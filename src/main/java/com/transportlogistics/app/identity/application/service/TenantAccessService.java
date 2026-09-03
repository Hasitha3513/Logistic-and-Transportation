package com.transportlogistics.app.identity.application.service;

import com.transportlogistics.app.identity.TenantAccessResolver;
import com.transportlogistics.app.identity.TenantMembershipManager;
import com.transportlogistics.app.identity.application.ports.out.TenantMembershipRepository;
import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import com.transportlogistics.app.identity.domain.model.TenantMembership;
import com.transportlogistics.app.identity.domain.model.TenantMembershipStatus;
import com.transportlogistics.app.tenancy.TenantDirectory;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public final class TenantAccessService implements TenantAccessResolver, TenantMembershipManager {
    private final TenantMembershipRepository memberships;
    private final TenantDirectory tenants;
    private final Clock clock;

    public TenantAccessService(TenantMembershipRepository memberships, TenantDirectory tenants, Clock clock) {
        this.memberships = memberships;
        this.tenants = tenants;
        this.clock = clock;
    }

    @Override
    public ResolvedTenant resolve(UUID userId) {
        var membership = memberships.findByUserId(userId)
                .orElseThrow(() -> denied("TENANT_MEMBERSHIP_NOT_FOUND"));
        if (membership.status() != TenantMembershipStatus.ACTIVE) {
            throw denied("TENANT_MEMBERSHIP_INACTIVE");
        }
        var tenant = tenants.findTenant(membership.tenantId())
                .orElseThrow(() -> denied("TENANT_NOT_FOUND"));
        if (!tenant.active()) {
            throw denied("TENANT_INACTIVE");
        }
        return new ResolvedTenant(membership.membershipId(), membership.tenantId(), tenant);
    }

    @Override
    public void ensureActiveMembership(UUID userId, UUID tenantId, String actor) {
        var tenant = tenants.findTenant(tenantId).filter(TenantDirectory.TenantView::active)
                .orElseThrow(() -> new IllegalStateException("Canonical Tenant is missing or inactive"));
        var existing = memberships.findByUserId(userId);
        if (existing.isPresent()) {
            if (!existing.get().tenantId().equals(tenant.tenantId())) {
                throw new IllegalStateException("User already belongs to another Tenant");
            }
            if (existing.get().status() == TenantMembershipStatus.ACTIVE) {
                return;
            }
        }
        var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        var id = existing.map(TenantMembership::membershipId).orElseGet(() -> UUID.nameUUIDFromBytes(
                ("tenant-membership:" + tenantId + ":" + userId).getBytes(StandardCharsets.UTF_8)));
        var createdAt = existing.map(TenantMembership::createdAt).orElse(now);
        var createdBy = existing.map(TenantMembership::createdBy).orElse(actor);
        var version = existing.map(TenantMembership::version).orElse(0L);
        memberships.save(new TenantMembership(id, tenantId, userId, TenantMembershipStatus.ACTIVE, createdAt,
                createdBy, now, actor, version));
    }

    private AuthenticationFailedException denied(String code) {
        return new AuthenticationFailedException(code);
    }
}
