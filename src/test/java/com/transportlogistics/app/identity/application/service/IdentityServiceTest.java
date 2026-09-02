package com.transportlogistics.app.identity.application.service;

import com.transportlogistics.app.identity.application.ports.out.AccessTokenService;
import com.transportlogistics.app.identity.TenantAccessResolver;
import com.transportlogistics.app.identity.TenantMembershipManager;
import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.application.ports.out.PasswordHasher;
import com.transportlogistics.app.identity.application.ports.out.RefreshTokenStore;
import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import com.transportlogistics.app.identity.domain.AuthorizationDeniedException;
import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase.AdministrationContext;
import com.transportlogistics.app.identity.domain.model.IssuedRefreshToken;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
    @Mock IdentityRepository repository;
    @Mock PasswordHasher passwords;
    @Mock AccessTokenService accessTokens;
    @Mock RefreshTokenStore refreshTokens;
    @Mock TenantAccessResolver tenantAccess;
    @Mock TenantMembershipManager tenantMemberships;
    private IdentityService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new IdentityService(repository, passwords, accessTokens, refreshTokens, tenantAccess, tenantMemberships,
                Duration.ofDays(30), clock);
    }

    @Test
    void authenticatesActiveUserAndIssuesExpiringTokens() {
        var user = user(true);
        var refresh = new IssuedRefreshToken("refresh", OffsetDateTime.parse("2026-01-31T00:00:00Z"));
        when(repository.findUserByUsername("operator")).thenReturn(Optional.of(user));
        when(passwords.matches("correct-password", user.passwordHash())).thenReturn(true);
        when(refreshTokens.issue(user.id(), refresh.expiresAt())).thenReturn(refresh);
        when(accessTokens.issue(user)).thenReturn("access");
        when(accessTokens.ttlSeconds()).thenReturn(900L);

        var result = service.login("operator", "correct-password");

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(result.expiresIn()).isEqualTo(900);
    }

    @Test
    void rejectsDisabledUserEvenWithCorrectPassword() {
        var user = user(false);
        when(repository.findUserByUsername("operator")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login("operator", "correct-password"))
                .isInstanceOf(AuthenticationFailedException.class);
        verifyNoInteractions(accessTokens, refreshTokens);
    }

    @Test
    void refreshRotatesTokenAndRejectsUnavailableUser() {
        var user = user(false);
        var replacement = new IssuedRefreshToken("replacement", OffsetDateTime.parse("2026-01-31T00:00:00Z"));
        when(refreshTokens.rotate(eq("old"), any(), eq(replacement.expiresAt())))
                .thenReturn(Optional.of(new RefreshTokenStore.Rotation(user.id(), replacement)));
        when(repository.findUser(user.id())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(AuthenticationFailedException.class);
        verify(refreshTokens).revoke(eq("replacement"), any());
        verify(accessTokens, never()).issue(any());
    }

    @Test
    void validatesAssignedRolesWhenCreatingUser() {
        var user = user(true);
        var requestedRole = UUID.randomUUID();
        when(repository.findRolesByIds(Set.of(requestedRole))).thenReturn(Set.of());

        assertThatThrownBy(() -> service.createUser(user, "a-strong-password", Set.of(requestedRole)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("roles");
        verify(repository, never()).saveUserWithRoles(any(), any());
    }

    @Test
    void createsAdministeredUserInsideActorsTenant() {
        var tenantId = UUID.randomUUID();
        var actor = new AdministrationContext(tenantId, "tenant.admin", Set.of("IDENTITY_MANAGE"));
        var user = user(true);
        var role = new Role(UUID.randomUUID(), "TENANT_ADMIN", null, true, Set.of("IDENTITY_MANAGE"));
        when(repository.findRolesByIds(Set.of(role.id()))).thenReturn(Set.of(role));
        when(passwords.hash("a-strong-password")).thenReturn("hash");
        when(repository.saveUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createUser(actor, user, "a-strong-password", Set.of(role.id()));

        verify(tenantMemberships).ensureActiveMembership(user.id(), tenantId, "tenant.admin");
        verify(repository).replaceUserRoles(user.id(), Set.of(role.id()));
    }

    @Test
    void rejectsRoleAssignmentAboveActorsPermissionCeiling() {
        var actor = new AdministrationContext(UUID.randomUUID(), "tenant.admin", Set.of("IDENTITY_MANAGE"));
        var role = new Role(UUID.randomUUID(), "APPROVER", null, true, Set.of("TRIP_APPROVE"));
        when(repository.findRolesByIds(Set.of(role.id()))).thenReturn(Set.of(role));

        assertThatThrownBy(() -> service.createUser(actor, user(true), "a-strong-password", Set.of(role.id())))
                .isInstanceOf(AuthorizationDeniedException.class);
        verify(repository, never()).saveUser(any());
    }

    private User user(boolean active) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var role = new Role(UUID.randomUUID(), "OPERATOR", null, true, Set.of("IDENTITY_READ"));
        return new User(UUID.randomUUID(), "operator", "operator@example.com", "$2a$12$hash", "Op", "Erator",
                null, active, now, now, Set.of(role));
    }
}
