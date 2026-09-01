package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRiderToBatchRequest(
        @NotNull(message = "Rider ID is required")
        UUID riderId,
        boolean isOverride,
        String overrideReason
) {}
