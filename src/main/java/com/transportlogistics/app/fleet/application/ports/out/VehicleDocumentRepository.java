package com.transportlogistics.app.fleet.application.ports.out;

import com.transportlogistics.app.fleet.domain.model.VehicleDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface VehicleDocumentRepository {
    VehicleDocument save(VehicleDocument document);

    Optional<VehicleDocument> findById(UUID id);

    List<VehicleDocument> findVisibleByVehicleId(UUID vehicleId);

    List<VehicleDocument> findActiveByVehicleId(UUID vehicleId);

    List<VehicleDocument> findActiveMandatoryExpiringBy(LocalDate cutoffInclusive);

    boolean activeDuplicateExists(UUID vehicleId, String documentType, String documentNumber, UUID excludedId);
}
