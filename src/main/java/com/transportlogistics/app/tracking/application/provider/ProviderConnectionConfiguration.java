package com.transportlogistics.app.tracking.application.provider;

import java.net.URI;
import java.util.Objects;

public record ProviderConnectionConfiguration(
        ProviderType providerType,
        URI endpoint,
        ProviderSafeConfiguration safeConfiguration) {

    public ProviderConnectionConfiguration {
        Objects.requireNonNull(providerType, "providerType");
        safeConfiguration = Objects.requireNonNullElseGet(
                safeConfiguration, ProviderSafeConfiguration::empty);
        if (endpoint != null && (!endpoint.isAbsolute() || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null)) {
            throw new IllegalArgumentException("Provider endpoint must be an absolute credential-free URI");
        }
    }
}
