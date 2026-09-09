package com.transportlogistics.app.tracking.application.provider;

import java.util.Objects;

public record TrackingProviderDescriptor(
        ProviderType providerType,
        ProviderCapabilities capabilities,
        boolean supported) {

    public TrackingProviderDescriptor {
        Objects.requireNonNull(providerType, "providerType");
        Objects.requireNonNull(capabilities, "capabilities");
    }
}
