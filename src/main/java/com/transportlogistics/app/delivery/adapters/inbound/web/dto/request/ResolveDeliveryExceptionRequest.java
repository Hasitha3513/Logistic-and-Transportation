package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionResolutionCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureDisposition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResolveDeliveryExceptionRequest(
        @NotNull(message = "Expected version is required")
        Long expectedVersion,
        @NotNull(message = "Resolution code is required")
        DeliveryExceptionResolutionCode resolutionCode,
        @NotBlank(message = "Resolution notes are required")
        @Size(max = 1000, message = "Resolution notes must not exceed 1000 characters")
        String resolutionNotes,
        UUID correctedLocationId,
        DeliveryFailureDisposition followUpDisposition
) {}
