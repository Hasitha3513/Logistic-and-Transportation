package com.transportlogistics.app.organization;

import java.util.Optional;
import java.util.UUID;

/** Minimal organization-owned vendor view exposed to other modules. */
public interface VendorLookup {
    Optional<VendorReference> find(UUID vendorId);

    record VendorReference(UUID id, String code, String name, boolean active) {
    }
}
