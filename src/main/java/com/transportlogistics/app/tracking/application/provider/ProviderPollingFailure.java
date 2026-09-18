package com.transportlogistics.app.tracking.application.provider;

/**
 * Provider-neutral, privacy-safe failure signal used by the polling coordinator.
 * Implementations must return only a bounded operational category, never provider payloads,
 * credentials, endpoint details, or exception text.
 */
public interface ProviderPollingFailure {
    String safeCode();
}
