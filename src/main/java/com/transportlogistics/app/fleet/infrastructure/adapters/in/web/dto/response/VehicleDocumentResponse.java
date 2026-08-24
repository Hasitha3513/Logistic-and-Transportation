package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.response;

import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleDocumentResponse(UUID id,
                                      UUID vehicleId,
                                      String documentType,
                                      String documentNumber,
                                      LocalDate issueDate,
                                      LocalDate expiryDate,
                                      String fileReference,
                                      boolean mandatoryForDispatch,
                                      VehicleDocumentStatus status,
                                      boolean active,
                                      OffsetDateTime createdAt,
                                      OffsetDateTime updatedAt,
                                      String createdBy,
                                      String updatedBy) {
}
