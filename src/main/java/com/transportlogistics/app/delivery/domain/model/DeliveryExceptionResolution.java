package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;

public record DeliveryExceptionResolution(
        DeliveryExceptionResolutionCode resolutionCode,
        String resolutionNotes,
        DeliveryFailureDisposition followUpDisposition,
        OffsetDateTime resolvedAt,
        String resolvedBy
) {
    public DeliveryExceptionResolution {
        if (resolutionCode == null) {
            throw new BusinessRuleException("INVALID_RESOLUTION", "Resolution code is required");
        }
        if (resolutionNotes == null || resolutionNotes.isBlank()) {
            throw new BusinessRuleException("INVALID_RESOLUTION_NOTES", "Resolution notes are required");
        }
        if (resolutionNotes.trim().length() > 1000) {
            throw new BusinessRuleException("INVALID_RESOLUTION_NOTES", "Resolution notes cannot exceed 1000 characters");
        }
        if (resolvedAt == null) {
            throw new BusinessRuleException("INVALID_RESOLUTION_TIME", "Resolution timestamp is required");
        }
        if (resolvedBy == null || resolvedBy.isBlank()) {
            throw new BusinessRuleException("INVALID_RESOLVER", "Resolver actor is required");
        }
        resolutionNotes = resolutionNotes.trim();
        resolvedBy = resolvedBy.trim();
    }
}
