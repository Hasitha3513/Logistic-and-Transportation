package com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectExceptionRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 2000)
        String reason,

        long version
) {}
