package com.transportlogistics.app.tracking.application.provider;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TrackingDeviceProviderBinding(
        UUID id,
        UUID tenantId,
        UUID trackingDeviceId,
        ProviderConnectionId providerConnectionId,
        String externalDeviceReference,
        ProviderSafeConfiguration safeConfiguration,
        DeviceProviderBindingLifecycle lifecycle,
        Instant watermarkSourceTimestamp,
        String watermarkMessageIdentity,
        Instant nextPollAt,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        long version) {

    public TrackingDeviceProviderBinding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(trackingDeviceId, "trackingDeviceId");
        Objects.requireNonNull(providerConnectionId, "providerConnectionId");
        externalDeviceReference = required(externalDeviceReference, "externalDeviceReference", 160);
        safeConfiguration = Objects.requireNonNullElseGet(
                safeConfiguration, ProviderSafeConfiguration::empty);
        Objects.requireNonNull(lifecycle, "lifecycle");
        watermarkMessageIdentity = optional(watermarkMessageIdentity, 160);
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(updatedAt, "updatedAt");
        Objects.requireNonNull(updatedBy, "updatedBy");
        if (version < 0) {
            throw new IllegalArgumentException("version cannot be negative");
        }
    }

    public static String required(String value, String name, int maximum) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.length() > maximum) {
            throw new IllegalArgumentException(name + " must be trimmed, nonblank and at most " + maximum);
        }
        return value;
    }

    public static String optional(String value, int maximum) {
        if (value == null) {
            return null;
        }
        return required(value, "watermarkMessageIdentity", maximum);
    }
}
