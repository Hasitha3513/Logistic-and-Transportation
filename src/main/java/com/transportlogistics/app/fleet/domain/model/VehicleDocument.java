package com.transportlogistics.app.fleet.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleDocument(UUID id, UUID vehicleId, String documentType, String documentNumber,
                              LocalDate issueDate, LocalDate expiryDate, String fileReference,
                              boolean mandatoryForDispatch, VehicleDocumentStatus status, boolean active,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt, String createdBy,
                              String updatedBy) {
    public VehicleDocument {
        if (id == null) throw new IllegalArgumentException("Document id is required");
        if (vehicleId == null) throw new IllegalArgumentException("Vehicle id is required");
        if (documentType == null || documentType.isBlank()) {
            throw new IllegalArgumentException("Document type is required");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("Document number is required");
        }
        if (issueDate != null && expiryDate != null && expiryDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Expiry date cannot precede issue date");
        }
        if (status == null) throw new IllegalArgumentException("Document status is required");
        if (active != (status == VehicleDocumentStatus.ACTIVE)) {
            throw new IllegalArgumentException("Active flag must match document status");
        }
        documentType = documentType.trim().toUpperCase();
        documentNumber = documentNumber.trim();
        fileReference = fileReference == null || fileReference.isBlank() ? null : fileReference.trim();
    }

    public boolean isExpiredOn(LocalDate date) {
        return active && expiryDate != null && expiryDate.isBefore(date);
    }

    public boolean blocksDispatchOn(LocalDate date) {
        return mandatoryForDispatch && isExpiredOn(date);
    }
}
