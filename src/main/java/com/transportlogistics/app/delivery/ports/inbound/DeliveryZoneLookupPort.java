package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneLookupPort {
    Optional<DeliveryZone> resolveZoneForLocation(UUID locationId);
    Optional<DeliveryZone> findZone(UUID id);
    Optional<DeliveryZone> findZoneForUpdate(UUID id);
    boolean isLocationServiceable(UUID locationId);
    List<DeliveryZone> listActiveZones();
}
