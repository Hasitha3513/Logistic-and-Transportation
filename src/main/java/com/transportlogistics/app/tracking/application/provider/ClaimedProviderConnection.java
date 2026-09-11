package com.transportlogistics.app.tracking.application.provider;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ClaimedProviderConnection(
        ProviderConnectionId id,
        UUID tenantId,
        ProviderType providerType,
        String providerAlias,
        String credentialReference,
        URI endpoint,
        ProviderSafeConfiguration safeConfiguration,
        int pollIntervalSeconds,
        int pageSize,
        String leaseOwner,
        Instant leaseUntil) {

    public ClaimedProviderConnection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(providerType, "providerType");
        Objects.requireNonNull(leaseOwner, "leaseOwner");
        Objects.requireNonNull(leaseUntil, "leaseUntil");
    }

    public ProviderConnectionExecution execution() {
        return new ProviderConnectionExecution(
                id, providerType, providerAlias, endpoint, safeConfiguration);
    }
}
