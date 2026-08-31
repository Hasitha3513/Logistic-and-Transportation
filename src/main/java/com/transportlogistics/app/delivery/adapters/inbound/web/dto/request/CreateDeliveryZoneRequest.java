package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryZoneCoordinate;
import com.transportlogistics.app.delivery.domain.model.DeliveryZoneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateDeliveryZoneRequest(
        @NotBlank @Size(max = 30) String zoneCode,
        @NotBlank @Size(max = 120) String zoneName,
        @Size(max = 500) String description,
        @NotNull DeliveryZoneType zoneType,
        Boolean serviceable,
        Integer dailyCapacity,
        UUID depotLocationId,
        @NotEmpty List<DeliveryZoneCoordinate> coordinates,
        Integer priority
) {}
