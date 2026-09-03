package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EscalateDeliveryRequest(
        @NotNull(message = "expectedVersion is required")
        Long expectedVersion,

        UUID deliveryAttemptId,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason cannot exceed 500 characters")
        String reason
) {}
