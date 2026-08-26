package com.transportlogistics.app.notification.infrastructure.testing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Mutable only in the isolated e2e profile so retry time can advance without real waiting. */
public final class E2eAdjustableClock extends Clock {
    private final Clock base;
    private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

    public E2eAdjustableClock(Clock base) {
        this.base = Objects.requireNonNull(base);
    }

    public void advance(Duration duration) {
        if (duration == null || duration.isNegative()) throw new IllegalArgumentException("duration must not be negative");
        offset.updateAndGet(current -> current.plus(duration));
    }

    public void reset() {
        offset.set(Duration.ZERO);
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.offset(base.withZone(zone), offset.get());
    }

    @Override
    public Instant instant() {
        return base.instant().plus(offset.get());
    }
}
