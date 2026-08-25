package com.transportlogistics.app.routing.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ApplyOptimizationRequest(
        @NotNull List<UUID> optimizedStopLocationIds
) {
}
