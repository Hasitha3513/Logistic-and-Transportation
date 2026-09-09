package com.transportlogistics.app.tracking.application.provider;

import java.util.List;
import java.util.Objects;

public record DiscoveryResult(Status status, List<DiscoveredDevice> devices, String nextCursor) {
    public enum Status { SUCCESS, UNSUPPORTED }

    public DiscoveryResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(devices, "devices");
        if (devices.size() > 500 || devices.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Discovery result exceeds 500 devices");
        }
        if (status == Status.UNSUPPORTED && (!devices.isEmpty() || nextCursor != null)) {
            throw new IllegalArgumentException("Unsupported discovery cannot contain results");
        }
        if (nextCursor != null && (nextCursor.isBlank() || nextCursor.length() > 160)) {
            throw new IllegalArgumentException("Discovery cursor is invalid");
        }
        devices = List.copyOf(devices);
    }

    public static DiscoveryResult unsupported() {
        return new DiscoveryResult(Status.UNSUPPORTED, List.of(), null);
    }

    public record DiscoveredDevice(
            String externalDeviceReference,
            String displayName,
            ProviderCapabilities capabilities) {

        public DiscoveredDevice {
            if (!bounded(externalDeviceReference, 160) || !bounded(displayName, 120)) {
                throw new IllegalArgumentException("Discovered device is invalid");
            }
            Objects.requireNonNull(capabilities, "capabilities");
        }

        private static boolean bounded(String value, int maximum) {
            return value != null && !value.isBlank() && value.length() <= maximum;
        }
    }
}
