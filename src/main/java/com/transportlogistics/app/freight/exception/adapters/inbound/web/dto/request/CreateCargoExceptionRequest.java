package com.transportlogistics.app.freight.exception.adapters.inbound.web.dto.request;

import com.transportlogistics.app.freight.exception.domain.ExceptionSeverity;
import com.transportlogistics.app.freight.exception.domain.ExceptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCargoExceptionRequest(
        @NotNull(message = "Exception type is required")
        ExceptionType exceptionType,

        ExceptionSeverity severity,

        @NotNull(message = "Freight order ID is required")
        UUID freightOrderId,

        UUID manifestId,
        UUID manifestItemId,

        @NotBlank(message = "Description is required")
        @Size(max = 2000)
        String description,

        @Size(max = 2000)
        String impact,

        @Size(max = 1000)
        String restriction,

        @Size(max = 2000)
        String correctiveAction
) {}
