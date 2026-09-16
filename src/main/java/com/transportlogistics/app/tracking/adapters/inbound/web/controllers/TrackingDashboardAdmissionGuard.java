package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.TooManyRequestsException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
final class TrackingDashboardAdmissionGuard {
    static final int ACTOR_LIMIT = 10;
    static final int TENANT_LIMIT = 40;
    private final ConcurrentHashMap<ActorWindow, AtomicInteger> actors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TenantWindow, AtomicInteger> tenants = new ConcurrentHashMap<>();
    private final Clock clock;
    private final MeterRegistry meters;

    TrackingDashboardAdmissionGuard(Clock clock, MeterRegistry meters) {
        this.clock = clock;
        this.meters = meters;
    }

    Timer.Sample admit(UUID tenantId, UUID actorId) {
        long minute = clock.instant().getEpochSecond() / 60;
        int actor = actors.computeIfAbsent(new ActorWindow(tenantId, actorId, minute), ignored -> new AtomicInteger())
                .incrementAndGet();
        int tenant = tenants.computeIfAbsent(new TenantWindow(tenantId, minute), ignored -> new AtomicInteger())
                .incrementAndGet();
        actors.keySet().removeIf(key -> key.minute < minute - 1);
        tenants.keySet().removeIf(key -> key.minute < minute - 1);
        if (actor > ACTOR_LIMIT || tenant > TENANT_LIMIT) {
            meters.counter("tracking.dashboard.rejected", "reason", "RATE_LIMITED").increment();
            throw new TooManyRequestsException("TRACKING_DASHBOARD_RATE_LIMITED", "Tracking dashboard request rate exceeded");
        }
        meters.counter("tracking.dashboard.requests").increment();
        return Timer.start(meters);
    }

    void complete(Timer.Sample sample, String outcome, int resultCount, String sourceStatus) {
        sample.stop(meters.timer("tracking.dashboard.latency", "outcome", outcome,
                "sourceStatus", sourceStatus));
        meters.summary("tracking.dashboard.result_size").record(resultCount);
    }

    private record ActorWindow(UUID tenantId, UUID actorId, long minute) { }
    private record TenantWindow(UUID tenantId, long minute) { }
}
