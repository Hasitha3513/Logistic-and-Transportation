package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record AddOrdersToBatchRequest(
        @NotEmpty(message = "At least one delivery order ID is required")
        List<UUID> deliveryOrderIds
) {}
