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

class TrackingDashboardAdmissionGuardTest {
    @Test
    void enforcesActorAndTenantLimitsWithoutSensitiveMetricTags() {
        var meters = new SimpleMeterRegistry();
        var guard = new TrackingDashboardAdmissionGuard(
                Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"), ZoneOffset.UTC), meters);
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        for (int index = 0; index < TrackingDashboardAdmissionGuard.ACTOR_LIMIT; index++) {
            guard.complete(guard.admit(tenant, actor), "SUCCESS", 1, "AVAILABLE",
                    Set.of("GEOFENCE", "SPEED"));
        }
        assertThatThrownBy(() -> guard.admit(tenant, actor))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        error -> assertThat(error.code()).isEqualTo("TRACKING_DASHBOARD_RATE_LIMITED"));
        assertThat(meters.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                .allSatisfy(tag -> assertThat(tag.getValue())
                        .doesNotContain(tenant.toString()).doesNotContain(actor.toString())));
        assertThat(meters.get("tracking.dashboard.included").tag("category", "GEOFENCE")
                .counter().count()).isEqualTo(TrackingDashboardAdmissionGuard.ACTOR_LIMIT);
        guard.complete(io.micrometer.core.instrument.Timer.start(meters), "SUCCESS", 0,
                "DEGRADED", Set.of());
        assertThat(meters.get("tracking.dashboard.degraded").tag("reason", "REDIS_UNAVAILABLE")
                .counter().count()).isOne();
    }
}
