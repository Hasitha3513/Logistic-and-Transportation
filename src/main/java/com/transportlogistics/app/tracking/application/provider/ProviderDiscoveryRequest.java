package com.transportlogistics.app.tracking.application.provider;

import java.util.Objects;

public record ProviderDiscoveryRequest(
        ProviderConnectionExecution connection,
        int limit,
        String cursor) {

    public ProviderDiscoveryRequest {
        Objects.requireNonNull(connection, "connection");
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("Discovery limit must be 1..500");
        }
        if (cursor != null && (cursor.isBlank() || cursor.length() > 160)) {
            throw new IllegalArgumentException("Discovery cursor is invalid");
        }
    }
}
