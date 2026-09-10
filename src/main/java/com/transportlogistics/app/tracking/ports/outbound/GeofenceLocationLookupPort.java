package com.transportlogistics.app.tracking.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface GeofenceLocationLookupPort {
    Optional<LocationReference> findActive(UUID tenantId, UUID locationId);

    record LocationReference(UUID id, boolean active) {
    }
}
