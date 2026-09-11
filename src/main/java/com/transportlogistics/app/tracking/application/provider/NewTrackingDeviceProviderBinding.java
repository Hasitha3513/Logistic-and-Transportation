package com.transportlogistics.app.tracking.application.provider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NewTrackingDeviceProviderBinding(
        UUID tenantId,
        UUID trackingDeviceId,
        ProviderConnectionId providerConnectionId,
        String externalDeviceReference,
        ProviderSafeConfiguration safeConfiguration,
        DeviceProviderBindingLifecycle lifecycle,
        Instant nextPollAt,
        UUID actorId,
        Instant now) {

    public NewTrackingDeviceProviderBinding {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(trackingDeviceId, "trackingDeviceId");
        Objects.requireNonNull(providerConnectionId, "providerConnectionId");
        externalDeviceReference = TrackingDeviceProviderBinding.required(
                externalDeviceReference, "externalDeviceReference", 160);
        safeConfiguration = Objects.requireNonNullElseGet(
                safeConfiguration, ProviderSafeConfiguration::empty);
        Objects.requireNonNull(lifecycle, "lifecycle");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(now, "now");
    }
}
