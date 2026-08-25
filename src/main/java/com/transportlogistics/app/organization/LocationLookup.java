package com.transportlogistics.app.organization;

import java.util.Optional;
import java.util.UUID;

/**
 * Minimal organization-owned location view exposed to other modules.
 */
public interface LocationLookup {
    Optional<LocationReference> find(UUID locationId);

    record LocationReference(UUID id, String code, String name, String address, Double latitude, Double longitude, boolean active) {
    }
}
