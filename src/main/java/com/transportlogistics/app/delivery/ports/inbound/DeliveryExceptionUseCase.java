package com.transportlogistics.app.delivery.ports.inbound;

import com.transportlogistics.app.delivery.domain.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface DeliveryExceptionUseCase {
    DeliveryExceptionCase reportException(UUID deliveryOrderId, ReportCommand command, String actor);
    DeliveryExceptionCase investigateException(UUID deliveryOrderId, UUID exceptionId, long expectedVersion, String actor);
    DeliveryExceptionCase resolveException(UUID deliveryOrderId, UUID exceptionId, ResolveCommand command, String actor);
    DeliveryExceptionCase cancelException(UUID deliveryOrderId, UUID exceptionId, CancelCommand command, String actor);
    List<DeliveryExceptionCase> listExceptions(UUID deliveryOrderId);
    DeliveryExceptionCase getException(UUID deliveryOrderId, UUID exceptionId);

    record ReportCommand(
            UUID deliveryAttemptId,
            DeliveryExceptionType exceptionType,
            DeliveryExceptionSeverity severity,
            String description,
            UUID correctedLocationId,
            String otpAttemptReference,
            String deliveredItemsDescription,
            String undeliveredItemsDescription,
            BigDecimal quantityDelivered,
            BigDecimal quantityUndelivered,
            List<EvidenceUpload> evidenceList
    ) {}

    record EvidenceUpload(
            byte[] content,
            String originalFilename
    ) {}

    record ResolveCommand(
            long expectedVersion,
            DeliveryExceptionResolutionCode resolutionCode,
            String resolutionNotes,
            UUID correctedLocationId,
            DeliveryFailureDisposition followUpDisposition
    ) {}

    record CancelCommand(
            long expectedVersion,
            String reason
    ) {}
}
