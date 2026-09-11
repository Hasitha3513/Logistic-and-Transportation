package com.transportlogistics.app.tracking.application.provider;

import java.util.Objects;
import java.util.UUID;

public record ProviderConnectionId(UUID value) {
    public ProviderConnectionId {
        Objects.requireNonNull(value, "value");
    }
}
