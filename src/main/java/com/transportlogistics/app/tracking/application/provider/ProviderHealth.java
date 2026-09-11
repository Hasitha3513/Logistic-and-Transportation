package com.transportlogistics.app.tracking.application.provider;

import java.time.Instant;
import java.util.Objects;

public record ProviderHealth(
        State state,
        Instant lastSuccessfulOperation,
        String failureCategory) {

    public enum State { HEALTHY, DEGRADED, UNAVAILABLE, UNKNOWN }

    public ProviderHealth {
        Objects.requireNonNull(state, "state");
        if (failureCategory != null && (failureCategory.isBlank()
                || failureCategory.length() > 80)) {
            throw new IllegalArgumentException("Provider health failure category is invalid");
        }
    }

    public static ProviderHealth unknown() {
        return new ProviderHealth(State.UNKNOWN, null, null);
    }
}
