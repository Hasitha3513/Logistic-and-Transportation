package com.transportlogistics.app.tracking.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class GpsExceptionRequests {
    private GpsExceptionRequests() { }
    public record Acknowledge(@PositiveOrZero long expectedVersion,
                              @NotBlank @Size(max=500) String reason) { }
}
