package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnToBaseRequest(
        @NotNull(message = "expectedVersion is required")
        Long expectedVersion,

        @Size(max = 1000, message = "reason cannot exceed 1000 characters")
        String reason
) {}
