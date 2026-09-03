package com.transportlogistics.app.delivery.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryLocationLookupPort {
    Optional<LocationReference> findLocation(UUID locationId);

    record LocationReference(UUID locationId, String code, String name, String address, Double latitude, Double longitude, boolean active) {
        public LocationReference(UUID locationId, String code, String name, boolean active) {
            this(locationId, code, name, null, null, null, active);
        }
    }
}
