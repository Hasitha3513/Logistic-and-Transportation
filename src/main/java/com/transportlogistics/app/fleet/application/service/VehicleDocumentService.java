package com.transportlogistics.app.fleet.application.service;

import com.transportlogistics.app.fleet.application.ports.in.VehicleDocumentUseCase;
import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.application.ports.out.VehicleRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import com.transportlogistics.app.shared.domain.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class VehicleDocumentService implements VehicleDocumentUseCase {
    private final VehicleRepository vehicles;
    private final VehicleDocumentRepository documents;

    public VehicleDocumentService(VehicleRepository vehicles, VehicleDocumentRepository documents) {
        this.vehicles = vehicles;
        this.documents = documents;
    }

    @Override
    public List<VehicleDocument> list(UUID vehicleId) {
        requireVehicle(vehicleId);
        return documents.findVisibleByVehicleId(vehicleId);
    }

    @Override
    public VehicleDocument create(UUID vehicleId, CreateCommand command, String actor) {
        requireVehicle(vehicleId);
        var state = state(command.status(), command.active(), VehicleDocumentStatus.ACTIVE, true);
        rejectDeletedStatus(state.status());
        var now = OffsetDateTime.now();
        var document = new VehicleDocument(UUID.randomUUID(), vehicleId, command.documentType(),
                command.documentNumber(), command.issueDate(), command.expiryDate(), command.fileReference(),
                command.mandatoryForDispatch(), state.status(), state.active(), now, now, actor(actor), actor(actor));
        rejectDuplicate(document, null);
        return documents.save(document);
    }

    @Override
    public VehicleDocument update(UUID vehicleId, UUID documentId, UpdateCommand command, String actor) {
        requireVehicle(vehicleId);
        var current = requireDocument(vehicleId, documentId);
        if (current.status() == VehicleDocumentStatus.DELETED) {
            throw new NotFoundException("Vehicle document not found: " + documentId);
        }
        var state = state(command.status(), command.active(), current.status(), current.active());
        rejectDeletedStatus(state.status());
        var updated = new VehicleDocument(current.id(), current.vehicleId(),
                value(command.documentType(), current.documentType()),
                value(command.documentNumber(), current.documentNumber()),
                command.issueDate() == null ? current.issueDate() : command.issueDate(),
                command.expiryDate() == null ? current.expiryDate() : command.expiryDate(),
                command.fileReference() == null ? current.fileReference() : command.fileReference(),
                command.mandatoryForDispatch() == null ? current.mandatoryForDispatch() : command.mandatoryForDispatch(),
                state.status(), state.active(), current.createdAt(), OffsetDateTime.now(), current.createdBy(), actor(actor));
        rejectDuplicate(updated, documentId);
        return documents.save(updated);
    }

    @Override
    public void delete(UUID vehicleId, UUID documentId, String actor) {
        requireVehicle(vehicleId);
        var current = requireDocument(vehicleId, documentId);
        if (current.status() == VehicleDocumentStatus.DELETED) return;
        documents.save(new VehicleDocument(current.id(), current.vehicleId(), current.documentType(),
                current.documentNumber(), current.issueDate(), current.expiryDate(), current.fileReference(),
                current.mandatoryForDispatch(), VehicleDocumentStatus.DELETED, false, current.createdAt(),
                OffsetDateTime.now(), current.createdBy(), actor(actor)));
    }

    private void requireVehicle(UUID vehicleId) {
        if (vehicles.findById(vehicleId).isEmpty()) {
            throw new NotFoundException("Vehicle not found: " + vehicleId);
        }
    }

    private VehicleDocument requireDocument(UUID vehicleId, UUID documentId) {
        var document = documents.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Vehicle document not found: " + documentId));
        if (!vehicleId.equals(document.vehicleId())) {
            throw new NotFoundException("Vehicle document not found: " + documentId);
        }
        return document;
    }

    private void rejectDuplicate(VehicleDocument document, UUID excludedId) {
        if (document.active() && documents.activeDuplicateExists(document.vehicleId(), document.documentType(),
                document.documentNumber(), excludedId)) {
            throw new IllegalArgumentException("An active document with this type and number already exists for the vehicle");
        }
    }

    private void rejectDeletedStatus(VehicleDocumentStatus status) {
        if (status == VehicleDocumentStatus.DELETED) {
            throw new IllegalArgumentException("Use the delete operation to delete a vehicle document");
        }
    }

    private State state(VehicleDocumentStatus requestedStatus, Boolean requestedActive,
                        VehicleDocumentStatus currentStatus, boolean currentActive) {
        if (requestedStatus == null && requestedActive == null) return new State(currentStatus, currentActive);
        var status = requestedStatus != null ? requestedStatus
                : Boolean.TRUE.equals(requestedActive) ? VehicleDocumentStatus.ACTIVE : VehicleDocumentStatus.INACTIVE;
        var active = requestedActive != null ? requestedActive : status == VehicleDocumentStatus.ACTIVE;
        if (active != (status == VehicleDocumentStatus.ACTIVE)) {
            throw new IllegalArgumentException("Active flag must match document status");
        }
        return new State(status, active);
    }

    private String value(String requested, String current) {
        return requested == null ? current : requested;
    }

    private String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor;
    }

    private record State(VehicleDocumentStatus status, boolean active) {
    }
}
