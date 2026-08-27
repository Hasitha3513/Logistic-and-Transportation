package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WaiveFineRequest(
        @NotBlank(message = "Reason is required") String reason
) {
}
