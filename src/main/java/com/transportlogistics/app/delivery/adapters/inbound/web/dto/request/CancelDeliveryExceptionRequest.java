package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelDeliveryExceptionRequest(
        @NotNull(message = "Expected version is required")
        Long expectedVersion,
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        String reason
) {}
