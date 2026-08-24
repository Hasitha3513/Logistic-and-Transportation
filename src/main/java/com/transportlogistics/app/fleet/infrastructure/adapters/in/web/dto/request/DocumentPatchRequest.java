package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;

import java.time.LocalDate;

public record DocumentPatchRequest(String documentType,
                                   String documentNumber,
                                   LocalDate issueDate,
                                   LocalDate expiryDate,
                                   String fileReference,
                                   Boolean mandatoryForDispatch,
                                   VehicleDocumentStatus status,
                                   Boolean active) {
}
