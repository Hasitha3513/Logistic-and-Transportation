package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transportlogistics.app.shared.domain.TooManyRequestsException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JourneyReplayAdmissionGuardTest {
    @Test
    void enforcesTheFrozenActorLimitAndEmitsOnlySafeOperationalTags() {
        var meters = new SimpleMeterRegistry();
        var guard = new JourneyReplayAdmissionGuard(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), meters);
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        for (int request = 0; request < JourneyReplayAdmissionGuard.ACTOR_LIMIT; request++) {
            var sample = guard.admit(tenant, actor, "points");
            guard.success(sample, "points", "COMPLETE", 1000, Set.of());
        }

        assertThatThrownBy(() -> guard.admit(tenant, actor, "points"))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        error -> assertThat(error.code()).isEqualTo("JOURNEY_REPLAY_RATE_LIMITED"));
        assertThat(meters.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                .allSatisfy(tag -> assertThat(tag.getValue())
                        .doesNotContain(tenant.toString()).doesNotContain(actor.toString())));
    }

    @Test
    void enforcesTheFrozenTenantLimitAcrossIndependentActors() {
        var guard = new JourneyReplayAdmissionGuard(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                new SimpleMeterRegistry());
        UUID tenant = UUID.randomUUID();
        for (int request = 0; request < JourneyReplayAdmissionGuard.TENANT_LIMIT; request++) {
            guard.admit(tenant, UUID.randomUUID(), "stops");
        }

        assertThatThrownBy(() -> guard.admit(tenant, UUID.randomUUID(), "stops"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void returnsTheGovernedRetryAfterHeaderWithTheStandardErrorContract() {
        var controller = new JourneyReplayController(null, null, null, null, Clock.systemUTC(), null);
        var request = new MockHttpServletRequest("POST", "/api/v1/tracking/journey-replays/points/query");

        var response = controller.rateLimited(new TooManyRequestsException(
                "JOURNEY_REPLAY_RATE_LIMITED", "Journey replay request rate exceeded"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("JOURNEY_REPLAY_RATE_LIMITED");
    }
}
