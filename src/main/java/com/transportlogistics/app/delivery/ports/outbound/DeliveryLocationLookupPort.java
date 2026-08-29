package com.transportlogistics.app.delivery.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryLocationLookupPort {
    Optional<LocationReference> findLocation(UUID locationId);

    record LocationReference(UUID locationId, String code, String name, boolean active) {}
}
