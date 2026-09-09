package com.transportlogistics.app.tracking.application.provider;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public record TrackingProviderConnectionMutation(
        String credentialReference,
        ProviderType providerType,
        String displayName,
        URI endpoint,
        ProviderSafeConfiguration safeConfiguration,
        int pollIntervalSeconds,
        int pageSize,
        ProviderConnectionLifecycle lifecycle,
        ProviderConnectionTestStatus testStatus,
        Instant lastTestedAt,
        Instant lastSuccessfulPollAt,
        Instant lastProviderMessageAt,
        String lastErrorCategory,
        Instant nextPollAt,
        String leaseOwner,
        Instant leaseUntil) {

    public TrackingProviderConnectionMutation {
        TrackingProviderConnection.validateKey(
                credentialReference, "credentialReference", 160);
        Objects.requireNonNull(providerType, "providerType");
        TrackingProviderConnection.validateDisplayName(displayName);
        safeConfiguration = Objects.requireNonNullElseGet(
                safeConfiguration, ProviderSafeConfiguration::empty);
        new ProviderConnectionConfiguration(providerType, endpoint, safeConfiguration);
        if (pollIntervalSeconds < 5 || pollIntervalSeconds > 86_400
                || pageSize < 1 || pageSize > 500) {
            throw new IllegalArgumentException("Provider polling configuration is invalid");
        }
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(testStatus, "testStatus");
        TrackingProviderConnection.validateErrorCategory(lastErrorCategory);
        TrackingProviderConnection.validateLease(leaseOwner, leaseUntil);
    }
}
