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

    private User user(boolean active, Set<Role> roles) {
        var now = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        return new User(UUID.randomUUID(), "operator", "operator@example.com", "hash", "Op", "Erator",
                null, active, now, now, roles);
    }
}
