package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record InvestigateDeliveryExceptionRequest(
        @NotNull(message = "Expected version is required")
        Long expectedVersion
) {}
