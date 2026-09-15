package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.TooManyRequestsException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Process-local replay admission and privacy-safe operational telemetry. */
@Component
final class JourneyReplayAdmissionGuard {
    static final int ACTOR_LIMIT = 30;
    static final int TENANT_LIMIT = 120;
    private final ConcurrentHashMap<WindowKey, AtomicInteger> actorWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TenantWindowKey, AtomicInteger> tenantWindows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final MeterRegistry meters;

    JourneyReplayAdmissionGuard(Clock clock, MeterRegistry meters) {
        this.clock = clock;
        this.meters = meters;
    }

    Timer.Sample admit(UUID tenantId, UUID actorId, String operation) {
        long minute = clock.instant().getEpochSecond() / 60;
        int actorCount = actorWindows.computeIfAbsent(
                new WindowKey(tenantId, actorId, minute), ignored -> new AtomicInteger()).incrementAndGet();
        int tenantCount = tenantWindows.computeIfAbsent(
                new TenantWindowKey(tenantId, minute), ignored -> new AtomicInteger()).incrementAndGet();
        removeExpired(minute);
        if (actorCount > ACTOR_LIMIT || tenantCount > TENANT_LIMIT) {
            meters.counter("tracking.journey_replay.rejected", "reason", "RATE_LIMITED",
                    "operation", operation).increment();
            throw new TooManyRequestsException("JOURNEY_REPLAY_RATE_LIMITED",
                    "Journey replay request rate exceeded");
        }
        meters.counter("tracking.journey_replay.requests", "operation", operation).increment();
        return Timer.start(meters);
    }

    void success(Timer.Sample sample, String operation, String coverage, int resultSize, Set<String> overlays) {
        sample.stop(meters.timer("tracking.journey_replay.latency", "operation", operation,
                "coverage", coverage));
        meters.summary("tracking.journey_replay.result_size", "operation", operation).record(resultSize);
        overlays.forEach(overlay -> meters.counter(
                "tracking.journey_replay.overlay", "type", overlay).increment());
    }

    void rejected(Timer.Sample sample, String operation, String reason) {
        sample.stop(meters.timer("tracking.journey_replay.latency", "operation", operation,
                "coverage", "REJECTED"));
        meters.counter("tracking.journey_replay.rejected", "reason", reason,
                "operation", operation).increment();
    }

    private void removeExpired(long currentMinute) {
        if ((currentMinute & 15) == 0) {
            actorWindows.keySet().removeIf(key -> key.minute < currentMinute - 1);
            tenantWindows.keySet().removeIf(key -> key.minute < currentMinute - 1);
        }
    }

    private record WindowKey(UUID tenantId, UUID actorId, long minute) { }
    private record TenantWindowKey(UUID tenantId, long minute) { }
}
