package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;
import com.transportlogistics.app.shared.domain.ConflictException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DeliveryExceptionCase(
        UUID id,
        DeliveryId deliveryOrderId,
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
        DeliveryExceptionResolution resolution,
        long version,
        OffsetDateTime reportedAt,
        String reportedBy,
        OffsetDateTime resolvedAt,
        String resolvedBy,
        List<DeliveryExceptionEvidence> evidence
) {
    public DeliveryExceptionCase {
        if (id == null || deliveryOrderId == null || exceptionType == null || severity == null
                || status == null || reportedAt == null || reportedBy == null || reportedBy.isBlank() || version < 0) {
            throw new BusinessRuleException("INVALID_EXCEPTION_CASE", "Required exception case data is missing");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessRuleException("INVALID_EXCEPTION_DESCRIPTION", "Exception description is required");
        }
        if (description.trim().length() > 1000) {
            throw new BusinessRuleException("INVALID_EXCEPTION_DESCRIPTION", "Exception description cannot exceed 1000 characters");
        }
        description = description.trim();
        reportedBy = reportedBy.trim();
        resolvedBy = resolvedBy == null || resolvedBy.isBlank() ? null : resolvedBy.trim();
        otpAttemptReference = otpAttemptReference == null || otpAttemptReference.isBlank() ? null : otpAttemptReference.trim();
        deliveredItemsDescription = deliveredItemsDescription == null || deliveredItemsDescription.isBlank() ? null : deliveredItemsDescription.trim();
        undeliveredItemsDescription = undeliveredItemsDescription == null || undeliveredItemsDescription.isBlank() ? null : undeliveredItemsDescription.trim();
        evidence = evidence == null ? List.of() : List.copyOf(evidence);

        if (status == DeliveryExceptionStatus.RESOLVED && (resolution == null || resolvedAt == null || resolvedBy == null)) {
            throw new BusinessRuleException("INVALID_RESOLVED_EXCEPTION", "Resolved exception case must contain complete resolution metadata");
        }
        if (status != DeliveryExceptionStatus.RESOLVED && resolution != null) {
            throw new BusinessRuleException("INVALID_UNRESOLVED_EXCEPTION", "Unresolved exception case must not contain resolution metadata");
        }
    }

    public static DeliveryExceptionCase create(
            UUID id,
            DeliveryId deliveryOrderId,
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
            List<DeliveryExceptionEvidence> initialEvidence,
            String actor,
            OffsetDateTime now
    ) {
        if (exceptionType == DeliveryExceptionType.DAMAGED_DELIVERY) {
            if (initialEvidence == null || initialEvidence.isEmpty()) {
                throw new BusinessRuleException("DELIVERY_EXCEPTION_EVIDENCE_REQUIRED", "At least one photo evidence is required for damaged delivery");
            }
            if (initialEvidence.size() > 3) {
                throw new ConflictException("DELIVERY_EXCEPTION_EVIDENCE_LIMIT_EXCEEDED", "A maximum of 3 photo evidences are allowed");
            }
        }
        if (exceptionType == DeliveryExceptionType.PARTIAL_DELIVERY) {
            if (quantityDelivered == null && quantityUndelivered == null
                    && deliveredItemsDescription == null && undeliveredItemsDescription == null) {
                throw new BusinessRuleException("INVALID_PARTIAL_DELIVERY_DATA", "Partial delivery requires delivered/undelivered item details or quantities");
            }
            if (quantityDelivered != null && quantityDelivered.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("INVALID_PARTIAL_QUANTITY", "Delivered quantity cannot be negative");
            }
            if (quantityUndelivered != null && quantityUndelivered.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("INVALID_PARTIAL_QUANTITY", "Undelivered quantity cannot be negative");
            }
        }

        return new DeliveryExceptionCase(
                id,
                deliveryOrderId,
                deliveryAttemptId,
                exceptionType,
                severity == null ? DeliveryExceptionSeverity.MEDIUM : severity,
                DeliveryExceptionStatus.OPEN,
                description,
                correctedLocationId,
                otpAttemptReference,
                deliveredItemsDescription,
                undeliveredItemsDescription,
                quantityDelivered,
                quantityUndelivered,
                null,
                0L,
                now,
                actor,
                null,
                null,
                initialEvidence
        );
    }

    public DeliveryExceptionCase investigate(String actor) {
        if (status == DeliveryExceptionStatus.RESOLVED || status == DeliveryExceptionStatus.CANCELLED) {
            throw new ConflictException("DELIVERY_EXCEPTION_ALREADY_TERMINAL", "Terminal exception case cannot be investigated");
        }
        return new DeliveryExceptionCase(
                id, deliveryOrderId, deliveryAttemptId, exceptionType, severity,
                DeliveryExceptionStatus.UNDER_INVESTIGATION, description,
                correctedLocationId, otpAttemptReference, deliveredItemsDescription,
                undeliveredItemsDescription, quantityDelivered, quantityUndelivered,
                resolution, version + 1, reportedAt, reportedBy, resolvedAt, resolvedBy, evidence
        );
    }

    public DeliveryExceptionCase resolve(
            DeliveryExceptionResolution resolution,
            UUID resolvedCorrectedLocationId,
            String actor,
            OffsetDateTime now
    ) {
        if (status == DeliveryExceptionStatus.RESOLVED || status == DeliveryExceptionStatus.CANCELLED) {
            throw new ConflictException("DELIVERY_EXCEPTION_ALREADY_TERMINAL", "Terminal exception case cannot be resolved");
        }
        if (resolution == null) {
            throw new BusinessRuleException("INVALID_RESOLUTION", "Resolution is required");
        }
        validateResolutionCodeForType(exceptionType, resolution.resolutionCode());

        UUID finalCorrectedLoc = resolvedCorrectedLocationId != null ? resolvedCorrectedLocationId : this.correctedLocationId;
        if (resolution.resolutionCode() == DeliveryExceptionResolutionCode.ADDRESS_CORRECTED && finalCorrectedLoc == null) {
            throw new BusinessRuleException("CORRECTED_LOCATION_REQUIRED", "Corrected destination location is required for address correction resolution");
        }

        return new DeliveryExceptionCase(
                id, deliveryOrderId, deliveryAttemptId, exceptionType, severity,
                DeliveryExceptionStatus.RESOLVED, description,
                finalCorrectedLoc, otpAttemptReference, deliveredItemsDescription,
                undeliveredItemsDescription, quantityDelivered, quantityUndelivered,
                resolution, version + 1, reportedAt, reportedBy, now, actor, evidence
        );
    }

    public DeliveryExceptionCase cancel(String cancellationReason, String actor, OffsetDateTime now) {
        if (status == DeliveryExceptionStatus.RESOLVED || status == DeliveryExceptionStatus.CANCELLED) {
            throw new ConflictException("DELIVERY_EXCEPTION_ALREADY_TERMINAL", "Terminal exception case cannot be cancelled");
        }
        return new DeliveryExceptionCase(
                id, deliveryOrderId, deliveryAttemptId, exceptionType, severity,
                DeliveryExceptionStatus.CANCELLED, description,
                correctedLocationId, otpAttemptReference, deliveredItemsDescription,
                undeliveredItemsDescription, quantityDelivered, quantityUndelivered,
                null, version + 1, reportedAt, reportedBy, now, actor, evidence
        );
    }

    public DeliveryExceptionCase addEvidence(DeliveryExceptionEvidence item) {
        if (status == DeliveryExceptionStatus.RESOLVED || status == DeliveryExceptionStatus.CANCELLED) {
            throw new ConflictException("DELIVERY_EXCEPTION_ALREADY_TERMINAL", "Cannot add evidence to terminal exception case");
        }
        if (evidence.size() >= 3) {
            throw new ConflictException("DELIVERY_EXCEPTION_EVIDENCE_LIMIT_EXCEEDED", "A maximum of 3 photo evidences are allowed");
        }
        var next = new ArrayList<>(evidence);
        next.add(item);
        return new DeliveryExceptionCase(
                id, deliveryOrderId, deliveryAttemptId, exceptionType, severity,
                status, description, correctedLocationId, otpAttemptReference,
                deliveredItemsDescription, undeliveredItemsDescription, quantityDelivered,
                quantityUndelivered, resolution, version + 1, reportedAt, reportedBy,
                resolvedAt, resolvedBy, next
        );
    }

    public boolean isBlockingPodFinalization() {
        return (status == DeliveryExceptionStatus.OPEN || status == DeliveryExceptionStatus.UNDER_INVESTIGATION)
                && (exceptionType == DeliveryExceptionType.OTP_MISMATCH || exceptionType == DeliveryExceptionType.DAMAGED_DELIVERY);
    }

    private static void validateResolutionCodeForType(DeliveryExceptionType type, DeliveryExceptionResolutionCode code) {
        boolean valid = switch (type) {
            case DAMAGED_DELIVERY -> code == DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED
                    || code == DeliveryExceptionResolutionCode.ACCEPTED_AS_IS
                    || code == DeliveryExceptionResolutionCode.REDELIVERY_APPROVED;
            case WRONG_ADDRESS -> code == DeliveryExceptionResolutionCode.ADDRESS_CORRECTED
                    || code == DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED;
            case PARTIAL_DELIVERY -> code == DeliveryExceptionResolutionCode.PARTIAL_ACCEPTED_CLOSE
                    || code == DeliveryExceptionResolutionCode.REDELIVERY_APPROVED
                    || code == DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED;
            case OTP_MISMATCH -> code == DeliveryExceptionResolutionCode.OTP_OVERRIDDEN_BY_MANAGER
                    || code == DeliveryExceptionResolutionCode.NEW_OTP_REQUESTED
                    || code == DeliveryExceptionResolutionCode.RETURN_TO_BASE_APPROVED;
            case RECIPIENT_REFUSAL -> code == DeliveryExceptionResolutionCode.REFUSAL_CONFIRMED_RTO
                    || code == DeliveryExceptionResolutionCode.REDELIVERY_APPROVED;
        };
        if (!valid) {
            throw new BusinessRuleException("INVALID_RESOLUTION_CODE_FOR_TYPE",
                    "Resolution code " + code + " is not valid for exception type " + type);
        }
    }
}
