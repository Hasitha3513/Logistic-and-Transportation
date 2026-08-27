package com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveExceptionRequest(
        @NotBlank(message = "Resolution description is required")
        @Size(max = 2000)
        String resolution,

        @Size(max = 2000)
        String correctiveAction,

        @Size(max = 2000)
        String reason,

        long version
) {}
