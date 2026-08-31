package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryOrderResponse(UUID id, String deliveryNumber, UUID customerId, UUID originLocationId,
                                    UUID destinationLocationId, DeliveryPriority priority,
                                    DeliveryServiceType serviceType, OffsetDateTime windowStart,
                                    OffsetDateTime windowEnd, String instructions, DeliveryStatus status,
                                    long version, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                    String createdBy, String updatedBy) {
    public static DeliveryOrderResponse from(DeliveryOrder value) {
        return new DeliveryOrderResponse(value.id().value(), value.deliveryNumber().value(), value.customerId(),
                value.originLocationId(), value.destinationLocationId(), value.priority(), value.serviceType(),
                value.window().start(), value.window().end(), value.instructions(), value.status(), value.version(),
                value.createdAt(), value.updatedAt(), value.createdBy(), value.updatedBy());
    }
}
