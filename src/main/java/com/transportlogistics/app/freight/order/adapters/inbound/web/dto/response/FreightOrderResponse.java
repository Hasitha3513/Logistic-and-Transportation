package com.transportlogistics.app.freight.order.adapters.inbound.web.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FreightOrderResponse(UUID id, String orderNumber, UUID customerId, UUID originLocationId,
                                   UUID destinationLocationId, OffsetDateTime requestedPickupAt,
                                   OffsetDateTime requestedDeliveryAt, String serviceLevel, String priority,
                                   String specialHandlingInstructions, List<FreightOrderLineResponse> lines,
                                   long version, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                   String createdBy, String updatedBy) { }
