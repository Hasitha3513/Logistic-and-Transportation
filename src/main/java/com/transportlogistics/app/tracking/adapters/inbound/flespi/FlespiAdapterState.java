package com.transportlogistics.app.tracking.adapters.inbound.flespi;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
final class FlespiAdapterState {
    enum FailureCategory { NONE, CONFIGURATION, AUTHENTICATION, PROVIDER, MAPPING, DOWNSTREAM }

    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(
            new Snapshot(false, false, null, null, FailureCategory.NONE));

    Snapshot snapshot() { return snapshot.get(); }

    void disabled(boolean configured) {
        snapshot.set(new Snapshot(configured, false, null, null,
                configured ? FailureCategory.NONE : FailureCategory.CONFIGURATION));
    }

    void successful(Instant poll, Instant providerMessage) {
        snapshot.set(new Snapshot(true, true, poll, providerMessage, FailureCategory.NONE));
    }

    void failed(FailureCategory category) {
        Snapshot current = snapshot.get();
        snapshot.set(new Snapshot(current.configured(), false, current.lastSuccessfulPoll(),
                current.lastProviderMessage(), category));
    }

    record Snapshot(boolean configured, boolean reachable, Instant lastSuccessfulPoll,
                    Instant lastProviderMessage, FailureCategory lastFailureCategory) {}
}
