package com.transportlogistics.app.shared;

/** Persists an integration event in the caller's transaction. */
public interface DurableEventPublisher {
    void publish(DurableEventEnvelope event);
}
