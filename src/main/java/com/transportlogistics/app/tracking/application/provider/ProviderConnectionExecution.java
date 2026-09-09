package com.transportlogistics.app.tracking.application.provider;

import java.net.URI;
import java.util.Objects;

public record ProviderConnectionExecution(
        ProviderConnectionId connectionId,
        ProviderType providerType,
        String providerAlias,
        URI endpoint,
        ProviderSafeConfiguration safeConfiguration) {

    public ProviderConnectionExecution {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(providerType, "providerType");
        if (providerAlias == null || !providerAlias.matches("[A-Z][A-Z0-9_]{0,79}")) {
            throw new IllegalArgumentException("Provider alias is invalid");
        }
        safeConfiguration = Objects.requireNonNullElseGet(
                safeConfiguration, ProviderSafeConfiguration::empty);
        if (endpoint != null && (!endpoint.isAbsolute() || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null)) {
            throw new IllegalArgumentException("Provider endpoint must be an absolute credential-free URI");
        }
    }
}
