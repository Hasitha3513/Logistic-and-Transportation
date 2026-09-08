package com.transportlogistics.app.tracking.adapters.inbound.web.controllers;

import com.transportlogistics.app.shared.domain.TooManyRequestsException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Process-local admission guard; the authenticated Tenant/provider pair is the authority. */
@Component
final class TrackingIngressGuard {
    private final ConcurrentHashMap<Key, Window> windows = new ConcurrentHashMap<>();
    private final int maximumPositionsPerSecond;
    private final Clock clock;
    private final MeterRegistry meters;

    TrackingIngressGuard(
            @Value("${app.tracking.ingress.max-positions-per-second:5000}") int maximumPositionsPerSecond,
            Clock clock,
            MeterRegistry meters) {
        this.maximumPositionsPerSecond = maximumPositionsPerSecond;
        this.clock = clock;
        this.meters = meters;
    }

    void admit(UUID tenantId, String providerAlias, int positions) {
        var second = clock.instant().getEpochSecond();
        var key = new Key(tenantId, providerAlias.toUpperCase(java.util.Locale.ROOT));
        var window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.epochSecond != second) {
                return new Window(second, new AtomicLong(positions));
            }
            current.positions.addAndGet(positions);
            return current;
        });
        meters.counter("tracking.ingress.received", "provider", key.providerAlias)
                .increment(positions);
        if (window.positions.get() > maximumPositionsPerSecond) {
            meters.counter("tracking.ingress.rate_limited", "provider", key.providerAlias).increment();
            throw new TooManyRequestsException("TRACKING_RATE_LIMITED", "Tracking ingestion rate exceeded");
        }
        removeExpired(second);
    }

    private void removeExpired(long currentSecond) {
        if ((currentSecond & 63) == 0) {
            windows.entrySet().removeIf(entry -> entry.getValue().epochSecond < currentSecond - 1);
        }
    }

    private record Key(UUID tenantId, String providerAlias) {}

    private record Window(long epochSecond, AtomicLong positions) {}
}
