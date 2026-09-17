package com.transportlogistics.app.tracking.adapters.inbound.traccar;

import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderHealth;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
final class TraccarAdapterState {
    enum FailureCategory { NONE, CONFIGURATION, AUTHENTICATION, PROVIDER, MAPPING }

    private final Map<ProviderConnectionId, Snapshot> connections = new ConcurrentHashMap<>();

    void successful(ProviderConnectionId connectionId, Instant poll) {
        connections.put(connectionId, new Snapshot(true, poll, FailureCategory.NONE));
    }

    void failed(ProviderConnectionId connectionId, FailureCategory category) {
        connections.compute(connectionId, (ignored, current) -> new Snapshot(
                false, current == null ? null : current.lastSuccessfulPoll(), category));
    }

    ProviderHealth health(ProviderConnectionId connectionId) {
        Snapshot current = connections.get(connectionId);
        if (current == null) {
            return ProviderHealth.unknown();
        }
        return new ProviderHealth(
                current.reachable() ? ProviderHealth.State.HEALTHY : ProviderHealth.State.DEGRADED,
                current.lastSuccessfulPoll(),
                current.failureCategory() == FailureCategory.NONE
                        ? null : current.failureCategory().name());
    }

    private record Snapshot(
            boolean reachable, Instant lastSuccessfulPoll, FailureCategory failureCategory) { }
}
