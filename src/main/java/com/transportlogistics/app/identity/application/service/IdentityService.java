package com.transportlogistics.app.identity.application.service;

import com.transportlogistics.app.identity.application.ports.in.IdentityUseCase;
import com.transportlogistics.app.identity.TenantAccessResolver;
import com.transportlogistics.app.identity.TenantMembershipManager;
import com.transportlogistics.app.identity.application.ports.out.AccessTokenService;
import com.transportlogistics.app.identity.application.ports.out.IdentityRepository;
import com.transportlogistics.app.identity.application.ports.out.PasswordHasher;
import com.transportlogistics.app.identity.application.ports.out.RefreshTokenStore;
import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import com.transportlogistics.app.identity.domain.model.AuthTokens;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import com.transportlogistics.app.shared.domain.NotFoundException;
import com.transportlogistics.app.tenancy.CanonicalTenant;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class IdentityService implements IdentityUseCase {
    private final IdentityRepository repo;
    private final PasswordHasher passwords;
    private final AccessTokenService accessTokens;
    private final RefreshTokenStore refreshTokens;
    private final TenantAccessResolver tenantAccess;
    private final TenantMembershipManager tenantMemberships;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public IdentityService(IdentityRepository repo, PasswordHasher passwords, AccessTokenService accessTokens,
                           RefreshTokenStore refreshTokens, TenantAccessResolver tenantAccess,
                           TenantMembershipManager tenantMemberships,
                           Duration refreshTokenTtl, Clock clock) {
        this.repo = repo;
        this.passwords = passwords;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.tenantAccess = tenantAccess;
        this.tenantMemberships = tenantMemberships;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    public User createUser(User u, String rawPassword, Set<UUID> roleIds) {
        requirePassword(rawPassword);
        var roles = resolveRoles(roleIds);
        var secured = new User(u.id(), u.username(), u.email(), passwords.hash(rawPassword), u.firstName(),
                u.lastName(), u.phone(), u.active(), u.createdAt(), u.updatedAt(), roles);
        var created = repo.saveUser(secured);
        tenantMemberships.ensureActiveMembership(created.id(), CanonicalTenant.ID, created.username());
        repo.replaceUserRoles(created.id(), roles.stream().map(Role::id).collect(java.util.stream.Collectors.toSet()));
        return repo.findUser(created.id()).orElse(created);
    }

    public User getUser(UUID id) {
        return repo.findUser(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<User> listUsers() {
        return repo.findUsers();
    }

    public User updateUser(UUID id, User u, String rawPassword, Set<UUID> roleIds) {
        var existing = getUser(id);
        var roles = roleIds == null ? existing.roles() : resolveRoles(roleIds);
        var passwordHash = rawPassword == null || rawPassword.isBlank() ? existing.passwordHash() : passwords.hash(rawPassword);
        var updated = new User(id, u.username(), u.email(), passwordHash, u.firstName(), u.lastName(), u.phone(),
                u.active(), existing.createdAt(), u.updatedAt(), roles);
        return repo.saveUserWithRoles(updated,
                roles.stream().map(Role::id).collect(java.util.stream.Collectors.toSet()));
    }

    public void deactivateUser(UUID id) {
        var u = getUser(id);
        repo.saveUser(new User(u.id(), u.username(), u.email(), u.passwordHash(), u.firstName(), u.lastName(), u.phone(), false, u.createdAt(), now(), u.roles()));
    }

    public Role createRole(Role r) {
        return repo.saveRole(r);
    }

    public Role getRole(UUID id) {
        return repo.findRole(id).orElseThrow(() -> new NotFoundException("Role not found: " + id));
    }

    public List<Role> listRoles() {
        return repo.findRoles();
    }

    public Role updateRole(UUID id, Role r) {
        getRole(id);
        return repo.saveRole(r);
    }

    public void deleteRole(UUID id) {
        repo.deleteRole(id);
    }

    public Optional<User> findByUsername(String username) {
        return repo.findUserByUsername(username);
    }

    public AuthTokens login(String username, String password) {
        var user = repo.findUserByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));
        if (!user.active() || !passwords.matches(password, user.passwordHash())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }
        tenantAccess.resolve(user.id());
        return tokensFor(user, refreshTokens.issue(user.id(), now().plus(refreshTokenTtl)));
    }

    public AuthTokens refresh(String refreshToken) {
        var rotation = refreshTokens.rotate(refreshToken, now(), now().plus(refreshTokenTtl))
                .orElseThrow(() -> new AuthenticationFailedException("Refresh token is invalid, expired, or revoked"));
        var user = repo.findUser(rotation.userId()).filter(User::active).orElse(null);
        if (user == null) {
            refreshTokens.revoke(rotation.token().value(), now());
            throw new AuthenticationFailedException("User is disabled or unavailable");
        }
        tenantAccess.resolve(user.id());
        return tokensFor(user, rotation.token());
    }

    public void logout(String refreshToken) {
        if (!refreshTokens.revoke(refreshToken, now())) {
            throw new AuthenticationFailedException("Refresh token is invalid or already revoked");
        }
    }

    public User currentUser(String username) {
        return repo.findUserByUsername(username).filter(User::active)
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user is disabled or unavailable"));
    }

    private AuthTokens tokensFor(User user, com.transportlogistics.app.identity.domain.model.IssuedRefreshToken refresh) {
        return new AuthTokens(accessTokens.issue(user), refresh.value(), "Bearer", accessTokens.ttlSeconds());
    }

    private Set<Role> resolveRoles(Set<UUID> roleIds) {
        var requested = roleIds == null ? Set.<UUID>of() : Set.copyOf(roleIds);
        var roles = repo.findRolesByIds(requested);
        if (roles.size() != requested.size() || roles.stream().anyMatch(role -> !role.active())) {
            throw new IllegalArgumentException("One or more roles are missing or inactive");
        }
        return roles;
    }

    private void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 12) {
            throw new IllegalArgumentException("Password must contain at least 12 characters");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
