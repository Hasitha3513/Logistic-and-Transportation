package com.transportlogistics.app.integration.adapters.outbound.ratelimit;

import com.transportlogistics.app.integration.ports.outbound.IntegrationRateLimiter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
class InMemoryIntegrationRateLimiter implements IntegrationRateLimiter {
    private static final int LIMIT = 5;
    private final ConcurrentHashMap<Key, Deque<OffsetDateTime>> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean allow(UUID tenantId, UUID configurationId, String actor, OffsetDateTime now) {
        Deque<OffsetDateTime> window = attempts.computeIfAbsent(new Key(tenantId, configurationId, actor),
            ignored -> new ArrayDeque<>());
        synchronized (window) {
            OffsetDateTime cutoff = now.minusMinutes(1);
            while (!window.isEmpty() && !window.peekFirst().isAfter(cutoff)) window.removeFirst();
            if (window.size() >= LIMIT) return false;
            window.addLast(now);
            return true;
        }
    }

    private record Key(UUID tenantId, UUID configurationId, String actor) {}
}
