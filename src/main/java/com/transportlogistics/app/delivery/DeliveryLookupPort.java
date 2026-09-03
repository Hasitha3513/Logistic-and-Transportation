package com.transportlogistics.app.delivery;

import java.util.Optional;
import java.util.UUID;

/** Public, read-only Delivery module contract for future cross-module consumers. */
public interface DeliveryLookupPort {
    Optional<DeliveryReference> findReference(UUID deliveryId);
}
