package com.transportlogistics.app.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthorizationTest {
    @Test
    void grantsPermissionsOnlyThroughActiveRolesAndActiveUser() {
        var role = new Role(UUID.randomUUID(), "ADMIN", null, true, Set.of("IDENTITY_MANAGE"));
        var user = user(true, Set.of(role));

        assertThat(user.hasRole("admin")).isTrue();
        assertThat(user.hasPermission("IDENTITY_MANAGE")).isTrue();
        assertThat(user.hasPermission("UNKNOWN")).isFalse();
        assertThat(user(true, Set.of(new Role(role.id(), role.name(), null, false, role.permissions())))
                .hasPermission("IDENTITY_MANAGE")).isFalse();
        assertThat(user(false, Set.of(role)).hasPermission("IDENTITY_MANAGE")).isFalse();
    }

    @Test
    void userToStringMasksPasswordHash() {
        var user = user(true, Set.of());
        var str = user.toString();
        assertThat(str).contains("passwordHash=***");
        assertThat(str).doesNotContain("hash");
    }

    @Test
    void authTokensToStringMasksTokens() {
        var tokens = new AuthTokens("secret-access-token", "secret-refresh-token", "Bearer", 3600);
        var str = tokens.toString();
        assertThat(str).contains("accessToken=***");
        assertThat(str).contains("refreshToken=***");
        assertThat(str).doesNotContain("secret-access-token");
        assertThat(str).doesNotContain("secret-refresh-token");
    }

    @Test
    void issuedRefreshTokenToStringMasksValue() {
        var token = new IssuedRefreshToken("raw-refresh-token-value", OffsetDateTime.now());
        var str = token.toString();
        assertThat(str).contains("value=***");
        assertThat(str).doesNotContain("raw-refresh-token-value");
    }

    private User user(boolean active, Set<Role> roles) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new User(UUID.randomUUID(), "operator", "operator@example.com", "hash", "Op", "Erator",
                null, active, now, now, roles);
    }
}
