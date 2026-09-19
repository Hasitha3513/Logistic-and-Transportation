package com.transportlogistics.app.compliance.domain;

import java.time.Instant;
import java.util.Objects;

/** Immutable half-open effective interval {@code [effectiveFrom, effectiveTo)}. */
public record EffectiveTimeWindow(Instant effectiveFrom, Instant effectiveTo) {

    public EffectiveTimeWindow {
        Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must be after effectiveFrom");
        }
    }

    public boolean includes(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return !instant.isBefore(effectiveFrom) && (effectiveTo == null || instant.isBefore(effectiveTo));
    }
}
