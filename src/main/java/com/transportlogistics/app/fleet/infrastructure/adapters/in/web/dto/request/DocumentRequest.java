package com.transportlogistics.app.fleet.infrastructure.adapters.in.web.dto.request;

import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DocumentRequest(@NotBlank String documentType,
                              @NotBlank String documentNumber,
                              LocalDate issueDate,
                              LocalDate expiryDate,
                              String fileReference,
                              Boolean mandatoryForDispatch,
                              @NotNull VehicleDocumentStatus status,
                              Boolean active) {
}
