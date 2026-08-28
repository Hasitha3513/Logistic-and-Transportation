package com.transportlogistics.app.identity.application.service;

import com.transportlogistics.app.identity.application.ports.out.TenantMembershipRepository;
import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import com.transportlogistics.app.identity.domain.model.TenantMembership;
import com.transportlogistics.app.identity.domain.model.TenantMembershipStatus;
import com.transportlogistics.app.tenancy.TenantDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAccessServiceTest {
    @Mock TenantMembershipRepository memberships;
    @Mock TenantDirectory tenants;
    private TenantAccessService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TenantAccessService(memberships, tenants,
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void resolvesOnlyAnActiveMembershipToAnActiveTenant() {
        var membership = membership(TenantMembershipStatus.ACTIVE);
        when(memberships.findByUserId(userId)).thenReturn(Optional.of(membership));
        when(tenants.findTenant(tenantId)).thenReturn(Optional.of(tenant("ACTIVE")));

        var result = service.resolve(userId);

        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.membershipId()).isEqualTo(membership.membershipId());
    }

    @Test
    void eachUserResolvesOnlyTheirServerSideMembership() {
        var userB = UUID.randomUUID();
        var tenantB = UUID.randomUUID();
        var membershipA = membership(TenantMembershipStatus.ACTIVE);
        var now = OffsetDateTime.parse("2026-08-28T00:00:00Z");
        var membershipB = new TenantMembership(UUID.randomUUID(), tenantB, userB, TenantMembershipStatus.ACTIVE,
                now, "test", now, "test", 0);
        when(memberships.findByUserId(userId)).thenReturn(Optional.of(membershipA));
        when(memberships.findByUserId(userB)).thenReturn(Optional.of(membershipB));
        when(tenants.findTenant(tenantId)).thenReturn(Optional.of(tenant("ACTIVE")));
        when(tenants.findTenant(tenantB)).thenReturn(Optional.of(
                new TenantDirectory.TenantView(tenantB, "TENANT-B", "Tenant B", "LKR", "Asia/Colombo", "ACTIVE")));

        assertThat(service.resolve(userId).tenantId()).isEqualTo(tenantId).isNotEqualTo(tenantB);
        assertThat(service.resolve(userB).tenantId()).isEqualTo(tenantB).isNotEqualTo(tenantId);
    }

    @Test
    void rejectsMissingOrInactiveMembership() {
        when(memberships.findByUserId(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve(userId)).isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("TENANT_MEMBERSHIP_NOT_FOUND");

        when(memberships.findByUserId(userId)).thenReturn(Optional.of(membership(TenantMembershipStatus.INACTIVE)));
        assertThatThrownBy(() -> service.resolve(userId)).isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("TENANT_MEMBERSHIP_INACTIVE");
    }

    @Test
    void rejectsMissingOrInactiveTenant() {
        when(memberships.findByUserId(userId)).thenReturn(Optional.of(membership(TenantMembershipStatus.ACTIVE)));
        when(tenants.findTenant(tenantId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve(userId)).isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("TENANT_NOT_FOUND");

        when(tenants.findTenant(tenantId)).thenReturn(Optional.of(tenant("INACTIVE")));
        assertThatThrownBy(() -> service.resolve(userId)).isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("TENANT_INACTIVE");
    }

    private TenantMembership membership(TenantMembershipStatus status) {
        var now = OffsetDateTime.parse("2026-08-28T00:00:00Z");
        return new TenantMembership(UUID.randomUUID(), tenantId, userId, status, now, "test", now, "test", 0);
    }

    private TenantDirectory.TenantView tenant(String status) {
        return new TenantDirectory.TenantView(tenantId, "CLTS-LK", "CLTS", "LKR", "Asia/Colombo", status);
    }
}
