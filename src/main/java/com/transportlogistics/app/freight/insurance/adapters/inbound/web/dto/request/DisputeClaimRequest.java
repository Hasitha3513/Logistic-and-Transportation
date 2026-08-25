package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisputeClaimRequest(
        @NotBlank(message = "Dispute reason is required")
        @Size(max = 2000)
        String reason,

        @NotNull(message = "Version is required for optimistic concurrency")
        Long version
) {}
