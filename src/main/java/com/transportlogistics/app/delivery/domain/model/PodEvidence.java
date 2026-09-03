package com.transportlogistics.app.delivery.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PodEvidence(UUID id, PodEvidenceType type, String storageReference, String barcodeValue,
                          String contentType, long contentLength, String checksum, String originalFilename,
                          String captureSource, String createdBy, OffsetDateTime createdAt) {
    public PodEvidence {
        if (id == null || type == null || createdAt == null || blank(createdBy) || blank(captureSource)) invalid("Evidence metadata is incomplete");
        if (type == PodEvidenceType.BARCODE) {
            if (blank(barcodeValue) || storageReference != null) invalid("Barcode evidence is invalid");
        } else if (blank(storageReference) || blank(contentType) || contentLength <= 0 || blank(checksum)) {
            invalid("Binary evidence metadata is invalid");
        }
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void invalid(String message) { throw new BusinessRuleException("POD_EVIDENCE_INVALID", message); }
}
