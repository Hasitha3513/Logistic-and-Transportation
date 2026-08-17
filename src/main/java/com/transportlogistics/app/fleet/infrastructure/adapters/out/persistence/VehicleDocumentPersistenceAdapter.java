package com.transportlogistics.app.fleet.infrastructure.adapters.out.persistence;

import com.transportlogistics.app.fleet.application.ports.out.VehicleDocumentRepository;
import com.transportlogistics.app.fleet.domain.model.VehicleDocument;
import com.transportlogistics.app.fleet.domain.model.VehicleDocumentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class VehicleDocumentPersistenceAdapter implements VehicleDocumentRepository {
    private final VehicleDocumentJpaRepository repository;

    @Override
    public VehicleDocument save(VehicleDocument document) {
        var entity = new VehicleDocumentEntity();
        entity.setId(document.id());
        entity.setVehicleId(document.vehicleId());
        entity.setDocumentType(document.documentType());
        entity.setDocumentNumber(document.documentNumber());
        entity.setIssueDate(document.issueDate());
        entity.setExpiryDate(document.expiryDate());
        entity.setFileReference(document.fileReference());
        entity.setMandatoryForDispatch(document.mandatoryForDispatch());
        entity.setStatus(document.status());
        entity.setActive(document.active());
        entity.setCreatedAt(document.createdAt());
        entity.setUpdatedAt(document.updatedAt());
        entity.setCreatedBy(document.createdBy());
        entity.setUpdatedBy(document.updatedBy());
        return map(repository.save(entity));
    }

    @Override
    public Optional<VehicleDocument> findById(UUID id) {
        return repository.findById(id).map(this::map);
    }

    @Override
    public List<VehicleDocument> findVisibleByVehicleId(UUID vehicleId) {
        return repository.findByVehicleIdAndStatusNotOrderByCreatedAtDesc(vehicleId, VehicleDocumentStatus.DELETED)
                .stream().map(this::map).toList();
    }

    @Override
    public List<VehicleDocument> findActiveByVehicleId(UUID vehicleId) {
        return repository.findByVehicleIdAndActiveTrue(vehicleId).stream().map(this::map).toList();
    }

    @Override
    public boolean activeDuplicateExists(UUID vehicleId, String type, String number, UUID excludedId) {
        return repository.existsByVehicleIdAndDocumentTypeIgnoreCaseAndDocumentNumberIgnoreCaseAndActiveTrueAndIdNot(
                vehicleId, type, number, excludedId == null ? new UUID(0, 0) : excludedId);
    }

    private VehicleDocument map(VehicleDocumentEntity entity) {
        return new VehicleDocument(entity.getId(), entity.getVehicleId(), entity.getDocumentType(),
                entity.getDocumentNumber(), entity.getIssueDate(), entity.getExpiryDate(), entity.getFileReference(),
                entity.isMandatoryForDispatch(), entity.getStatus(), entity.isActive(), entity.getCreatedAt(),
                entity.getUpdatedAt(), entity.getCreatedBy(), entity.getUpdatedBy());
    }
}
