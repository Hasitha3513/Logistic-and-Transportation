package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryExceptionEvidence(
        UUID id,
        UUID exceptionCaseId,
        String storageReference,
        String detectedContentType,
        long contentLength,
        String sha256Checksum,
        String originalFilename,
        String createdBy,
        OffsetDateTime createdAt
) {
    public DeliveryExceptionEvidence {
        if (id == null || storageReference == null || storageReference.isBlank()
                || detectedContentType == null || detectedContentType.isBlank()
                || contentLength <= 0 || sha256Checksum == null || sha256Checksum.isBlank()
                || createdBy == null || createdBy.isBlank() || createdAt == null) {
            throw new BusinessRuleException("INVALID_EXCEPTION_EVIDENCE", "Evidence metadata is incomplete");
        }
        if (!detectedContentType.equals("image/png") && !detectedContentType.equals("image/jpeg")) {
            throw new BusinessRuleException("INVALID_EVIDENCE_TYPE", "Only JPEG and PNG evidence are accepted");
        }
        storageReference = storageReference.trim();
        detectedContentType = detectedContentType.trim();
        sha256Checksum = sha256Checksum.trim();
        originalFilename = originalFilename == null || originalFilename.isBlank() ? null : originalFilename.trim();
        createdBy = createdBy.trim();
    }
}
