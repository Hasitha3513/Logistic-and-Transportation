package com.transportlogistics.app.freight.order.adapters.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UpdateFreightOrderRequest(
        @NotNull @PositiveOrZero Long version,
        UUID customerId,
        UUID originLocationId,
        UUID destinationLocationId,
        OffsetDateTime requestedPickupAt,
        OffsetDateTime requestedDeliveryAt,
        @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*") String serviceLevel,
        @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*") String priority,
        @Size(max = 2000) String specialHandlingInstructions,
        @Size(min = 1) List<@Valid FreightOrderLineRequest> lines
) { }
