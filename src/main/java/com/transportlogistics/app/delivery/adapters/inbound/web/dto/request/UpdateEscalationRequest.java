package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryEscalationStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureDisposition;
import jakarta.validation.constraints.NotNull;

public record UpdateEscalationRequest(
        @NotNull(message = "status is required")
        DeliveryEscalationStatus status,

        String resolutionNotes,

        DeliveryFailureDisposition nextDisposition
) {}
