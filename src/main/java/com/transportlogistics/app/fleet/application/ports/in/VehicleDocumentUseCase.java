package com.transportlogistics.app.fleet.application.ports.in;

import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VehicleDocumentUseCase {
    List<VehicleDocument> list(UUID vehicleId);

    VehicleDocument create(UUID vehicleId, CreateCommand command, String actor);

    VehicleDocument update(UUID vehicleId, UUID documentId, UpdateCommand command, String actor);

    void delete(UUID vehicleId, UUID documentId, String actor);

    record CreateCommand(String documentType, String documentNumber, LocalDate issueDate, LocalDate expiryDate,
                         String fileReference, boolean mandatoryForDispatch, VehicleDocumentStatus status,
                         Boolean active) {
    }

    record UpdateCommand(String documentType, String documentNumber, LocalDate issueDate, LocalDate expiryDate,
                         String fileReference, Boolean mandatoryForDispatch, VehicleDocumentStatus status,
                         Boolean active) {
    }
}
