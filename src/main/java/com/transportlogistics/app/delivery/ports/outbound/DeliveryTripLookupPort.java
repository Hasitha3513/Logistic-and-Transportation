package com.transportlogistics.app.delivery.ports.outbound;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryTripLookupPort {
    Optional<TripDeliveryReference> findTrip(UUID tripId);

    record TripDeliveryReference(UUID tripId, String tripNumber, String status,
                                 UUID vehicleId, UUID driverId, UUID routeId) {}
}
