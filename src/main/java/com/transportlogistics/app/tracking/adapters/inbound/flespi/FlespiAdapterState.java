package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.transportlogistics.app.tracking.application.provider.ProviderConnectionId;
import com.transportlogistics.app.tracking.application.provider.ProviderHealth;
import org.springframework.stereotype.Component;

@Component
final class FlespiAdapterState {
    enum FailureCategory { NONE, CONFIGURATION, AUTHENTICATION, PROVIDER, MAPPING, DOWNSTREAM }

    private final Map<ProviderConnectionId, Snapshot> connections = new ConcurrentHashMap<>();

    Snapshot snapshot() {
        return connections.values().stream()
                .max(java.util.Comparator.comparing(Snapshot::lastSuccessfulPoll,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .orElse(new Snapshot(false, false, null, null, FailureCategory.NONE));
    }

    void successful(ProviderConnectionId connectionId, Instant poll, Instant providerMessage) {
        connections.put(connectionId,
                new Snapshot(true, true, poll, providerMessage, FailureCategory.NONE));
    }

    void failed(ProviderConnectionId connectionId, FailureCategory category) {
        connections.compute(connectionId, (ignored, current) -> new Snapshot(
                true, false,
                current == null ? null : current.lastSuccessfulPoll(),
                current == null ? null : current.lastProviderMessage(), category));
    }

    ProviderHealth health(ProviderConnectionId connectionId) {
        Snapshot current = connections.get(connectionId);
        if (current == null) {
            return ProviderHealth.unknown();
        }
        return new ProviderHealth(
                current.reachable() ? ProviderHealth.State.HEALTHY : ProviderHealth.State.DEGRADED,
                current.lastSuccessfulPoll(),
                current.lastFailureCategory() == FailureCategory.NONE
                        ? null : current.lastFailureCategory().name());
    }

    int connectionCount() { return connections.size(); }

    record Snapshot(boolean configured, boolean reachable, Instant lastSuccessfulPoll,
                    Instant lastProviderMessage, FailureCategory lastFailureCategory) {}
}
