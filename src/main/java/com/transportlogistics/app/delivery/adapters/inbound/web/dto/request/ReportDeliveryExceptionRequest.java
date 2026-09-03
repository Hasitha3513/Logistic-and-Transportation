package com.transportlogistics.app.delivery.adapters.inbound.web.dto.request;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionSeverity;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReportDeliveryExceptionRequest(
        UUID deliveryAttemptId,
        @NotNull(message = "Exception type is required")
        DeliveryExceptionType exceptionType,
        DeliveryExceptionSeverity severity,
        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,
        UUID correctedLocationId,
        String otpAttemptReference,
        String deliveredItemsDescription,
        String undeliveredItemsDescription,
        BigDecimal quantityDelivered,
        BigDecimal quantityUndelivered,
        List<EvidenceUploadItem> evidenceList
) {
    public record EvidenceUploadItem(
            String originalFilename,
            String base64Content
    ) {}
}
