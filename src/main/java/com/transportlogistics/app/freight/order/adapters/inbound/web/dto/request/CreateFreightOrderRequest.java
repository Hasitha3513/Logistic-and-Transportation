package com.transportlogistics.app.freight.order.adapters.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateFreightOrderRequest(
        @NotNull UUID customerId,
        @NotNull UUID originLocationId,
        @NotNull UUID destinationLocationId,
        @NotNull OffsetDateTime requestedPickupAt,
        @NotNull OffsetDateTime requestedDeliveryAt,
        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*") String serviceLevel,
        @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*") String priority,
        @Size(max = 2000) String specialHandlingInstructions,
        @NotEmpty List<@Valid FreightOrderLineRequest> lines
) { }
