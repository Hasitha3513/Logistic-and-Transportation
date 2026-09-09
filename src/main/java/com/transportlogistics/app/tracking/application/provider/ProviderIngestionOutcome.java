package com.transportlogistics.app.tracking.application.provider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProviderIngestionOutcome(
        UUID bindingId,
        long bindingVersion,
        Instant sourceTimestamp,
        String messageIdentity,
        Result result) {

    public ProviderIngestionOutcome {
        Objects.requireNonNull(bindingId, "bindingId");
        Objects.requireNonNull(sourceTimestamp, "sourceTimestamp");
        Objects.requireNonNull(result, "result");
    }

    public enum Result {
        ACCEPTED,
        DUPLICATE,
        REJECTED
    }
}
