package com.transportlogistics.app.delivery.adapters.inbound.web.dto.response;

import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionResolutionCode;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionSeverity;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionStatus;
import com.transportlogistics.app.delivery.domain.model.DeliveryExceptionType;
import com.transportlogistics.app.delivery.domain.model.DeliveryFailureDisposition;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DeliveryExceptionResponse(
        UUID id,
        UUID deliveryOrderId,
        UUID deliveryAttemptId,
        DeliveryExceptionType exceptionType,
        DeliveryExceptionSeverity severity,
        DeliveryExceptionStatus status,
        String description,
        UUID correctedLocationId,
        String otpAttemptReference,
        String deliveredItemsDescription,
        String undeliveredItemsDescription,
        BigDecimal quantityDelivered,
        BigDecimal quantityUndelivered,
        ResolutionInfo resolution,
        long version,
        OffsetDateTime reportedAt,
        String reportedBy,
        OffsetDateTime resolvedAt,
        String resolvedBy,
        List<EvidenceInfo> evidence
) {
    public record ResolutionInfo(
            DeliveryExceptionResolutionCode resolutionCode,
            String resolutionNotes,
            DeliveryFailureDisposition followUpDisposition,
            OffsetDateTime resolvedAt,
            String resolvedBy
    ) {}

    public record EvidenceInfo(
            UUID id,
            String storageReference,
            String detectedContentType,
            long contentLength,
            String sha256Checksum,
            String originalFilename,
            String createdBy,
            OffsetDateTime createdAt
    ) {}
}
