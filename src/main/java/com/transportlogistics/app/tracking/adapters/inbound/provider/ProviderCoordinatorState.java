package com.transportlogistics.app.tracking.adapters.inbound.provider;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public final class ProviderCoordinatorState {
    private final AtomicInteger activeJobs = new AtomicInteger();
    private final AtomicLong lastSuccessEpoch = new AtomicLong();
    private final AtomicLong lastFailureEpoch = new AtomicLong();

    void started() {
        activeJobs.incrementAndGet();
    }

    void finished(boolean successful, Instant now) {
        activeJobs.decrementAndGet();
        (successful ? lastSuccessEpoch : lastFailureEpoch).set(now.getEpochSecond());
    }

    public int activeJobs() {
        return activeJobs.get();
    }

    public long lastSuccessEpoch() {
        return lastSuccessEpoch.get();
    }

    public long lastFailureEpoch() {
        return lastFailureEpoch.get();
    }
}
