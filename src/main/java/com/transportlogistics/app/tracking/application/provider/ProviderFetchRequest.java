package com.transportlogistics.app.tracking.application.provider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProviderFetchRequest(
        ProviderConnectionExecution connection,
        List<ProviderDeviceCursor> devices,
        int pageLimit,
        int responseByteLimit,
        Instant deadline) {

    public ProviderFetchRequest {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(devices, "devices");
        if (devices.isEmpty() || devices.size() > 500 || devices.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Fetch request must contain 1..500 devices");
        }
        if (pageLimit < 1 || pageLimit > 500) {
            throw new IllegalArgumentException("Page limit must be 1..500");
        }
        if (responseByteLimit < 1 || responseByteLimit > 1_048_576) {
            throw new IllegalArgumentException("Response limit must be 1..1048576 bytes");
        }
        Objects.requireNonNull(deadline, "deadline");
        devices = List.copyOf(devices);
    }
}
