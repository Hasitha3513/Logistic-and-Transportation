package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryZone;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DeliveryZoneResponse(
        UUID id,
        UUID tenantId,
        String zoneCode,
        String zoneName,
        String description,
        DeliveryZoneType zoneType,
        DeliveryZoneStatus status,
        boolean serviceable,
        Integer dailyCapacity,
        UUID depotLocationId,
        List<DeliveryZoneCoordinate> coordinates,
        double minLatitude,
        double maxLatitude,
        double minLongitude,
        double maxLongitude,
        double approximateArea,
        int priority,
        Long version,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public static DeliveryZoneResponse fromDomain(DeliveryZone zone) {
        return new DeliveryZoneResponse(
                zone.id(),
                zone.tenantId(),
                zone.zoneCode(),
                zone.zoneName(),
                zone.description(),
                zone.zoneType(),
                zone.status(),
                zone.serviceable(),
                zone.dailyCapacity(),
                zone.depotLocationId(),
                zone.boundary().coordinates(),
                zone.boundary().boundingBox().minLatitude(),
                zone.boundary().boundingBox().maxLatitude(),
                zone.boundary().boundingBox().minLongitude(),
                zone.boundary().boundingBox().maxLongitude(),
                zone.boundary().approximateArea(),
                zone.priority(),
                zone.version(),
                zone.createdAt(),
                zone.createdBy(),
                zone.updatedAt(),
                zone.updatedBy()
        );
    }
}
