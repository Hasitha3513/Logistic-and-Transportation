package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryOrderResponse(UUID id, String deliveryNumber, UUID customerId, UUID originLocationId,
                                    UUID destinationLocationId, DeliveryPriority priority,
                                    DeliveryServiceType serviceType, OffsetDateTime windowStart,
                                    OffsetDateTime windowEnd, String instructions, DeliveryStatus status,
                                    long version, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                    String createdBy, String updatedBy) {}
