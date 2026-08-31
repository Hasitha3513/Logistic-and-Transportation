package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneUseCase {

    record CreateZoneCommand(
            String zoneCode,
            String zoneName,
            String description,
            DeliveryZoneType zoneType,
            boolean serviceable,
            Integer dailyCapacity,
            UUID depotLocationId,
            List<DeliveryZoneCoordinate> coordinates,
            int priority
    ) {}

    record UpdateZoneCommand(
            String zoneName,
            String description,
            DeliveryZoneType zoneType,
            boolean serviceable,
            Integer dailyCapacity,
            UUID depotLocationId,
            List<DeliveryZoneCoordinate> coordinates,
            int priority,
            long expectedVersion
    ) {}

    DeliveryZone createZone(CreateZoneCommand command, String actor);
    DeliveryZone updateZone(UUID id, UpdateZoneCommand command, String actor);
    DeliveryZone activateZone(UUID id, String actor);
    DeliveryZone deactivateZone(UUID id, String actor);
    DeliveryZone getZone(UUID id);
    List<DeliveryZone> listZones(DeliveryZoneStatus status, Boolean serviceable);
    Optional<DeliveryZone> resolveZoneForCoordinates(double longitude, double latitude);
    Optional<DeliveryZone> resolveZoneForLocation(UUID locationId);
    boolean isLocationServiceable(UUID locationId);
}
