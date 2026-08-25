package com.transportlogistics.app.organization;

import java.util.Optional;
import java.util.UUID;

/** Minimal organization-owned customer view exposed to other modules. */
public interface CustomerLookup {
    Optional<CustomerReference> find(UUID customerId);

    record CustomerReference(UUID id, String code, String name, boolean active) {
    }
}
