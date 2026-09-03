package com.transportlogistics.app.shared;

/** Canonical Tenant-scoped envelope for an internally durable integration event. */
public interface DurableEventEnvelope extends EventEnvelope {
    String durableConsumer();
}
