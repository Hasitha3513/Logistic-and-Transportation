package com.transportlogistics.app.freight.insurance.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApproveClaimRequest(
        @NotNull(message = "Version is required for optimistic concurrency")
        Long version
) {}
