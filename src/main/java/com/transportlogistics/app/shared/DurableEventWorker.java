package com.transportlogistics.app.shared;

/** Executes one bounded Tenant-scoped durable-event delivery batch. */
public interface DurableEventWorker {
    void processDue();
}
