package com.transportlogistics.app.identity.infrastructure.security;

import com.transportlogistics.app.identity.domain.AuthenticationFailedException;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAccessTokenServiceTest {
    private static final String SECRET = "test-secret-with-at-least-thirty-two-bytes";

    @Test
    void signedTokenContainsAuthorizationClaimsAndExpires() {
        var issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        var properties = new JwtProperties(SECRET, "test-issuer", Duration.ofMinutes(1), Duration.ofDays(30));
        var issuer = new JwtAccessTokenService(properties, Clock.fixed(issuedAt, ZoneOffset.UTC));
        var role = new Role(UUID.randomUUID(), "ADMIN", null, true, Set.of("IDENTITY_MANAGE"));
        var user = new User(UUID.randomUUID(), "admin", "admin@example.com", "hash", "Admin", "User", null,
                true, OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC), OffsetDateTime.ofInstant(issuedAt, ZoneOffset.UTC),
                Set.of(role));

        var token = issuer.issue(user);
        var validClaims = issuer.verify(token);

        assertThat(validClaims.username()).isEqualTo("admin");
        assertThat(validClaims.roles()).containsExactly("ADMIN");
        assertThat(validClaims.permissions()).containsExactly("IDENTITY_MANAGE");

        var expiredVerifier = new JwtAccessTokenService(properties,
                Clock.fixed(issuedAt.plusSeconds(61), ZoneOffset.UTC));
        assertThatThrownBy(() -> expiredVerifier.verify(token)).isInstanceOf(AuthenticationFailedException.class);
    }
}
