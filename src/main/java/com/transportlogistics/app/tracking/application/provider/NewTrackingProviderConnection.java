package com.transportlogistics.app.tracking.application.provider;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NewTrackingProviderConnection(
        UUID tenantId,
        UUID actorId,
        String providerKeyId,
        String providerAlias,
        String credentialReference,
        ProviderType providerType,
        String displayName,
        URI endpoint,
        ProviderSafeConfiguration safeConfiguration,
        int pollIntervalSeconds,
        int pageSize,
        ProviderConnectionLifecycle lifecycle,
        Instant now) {

    public NewTrackingProviderConnection {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(actorId, "actorId");
        TrackingProviderConnection.validateKey(providerKeyId, "providerKeyId", 160);
        TrackingProviderConnection.validateAlias(providerAlias);
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
        Objects.requireNonNull(now, "now");
    }
}
